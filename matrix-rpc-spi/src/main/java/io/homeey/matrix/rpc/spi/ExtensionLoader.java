package io.homeey.matrix.rpc.spi;

import io.homeey.matrix.rpc.common.Holder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author jt4mrg@gmail.com
 * @since 2025-12-31
 **/
public class ExtensionLoader<T> {
    private static final String MATRIX_DIRECTORY = "META-INF/matrix/";

    private static final Map<Class<?>, ExtensionLoader<?>> LOADERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    private final Class<T> type;

    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();
    private final Map<String, Holder<T>> cachedInstances = new ConcurrentHashMap<>();

    private ExtensionLoader(Class<T> type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException("SPI type must be interface.");
        }
        if (!type.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("SPI interface must be annotated with @SPI");
        }
        return (ExtensionLoader<T>) LOADERS.computeIfAbsent(type, ExtensionLoader::new);
    }

    public T getDefaultExtension() {
        SPI spi = type.getAnnotation(SPI.class);
        if (spi == null || spi.value().isEmpty()) {
            return null;
        }
        return getExtension(spi.value());
    }

    public T getExtension(String name) {
        Holder<T> holder = cachedInstances.computeIfAbsent(name, k -> new Holder<>());
        T instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new IllegalStateException("No such extension: " + name);
        }
        try {
            return (T) EXTENSION_INSTANCES.computeIfAbsent(clazz, c -> {
                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to create extension " + name, e);
        }
    }

    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    private Map<String, Class<?>> loadExtensionClasses() {
        Map<String, Class<?>> classes = new HashMap<>();

        String fileName = MATRIX_DIRECTORY + type.getName();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> urls = classLoader.getResources(fileName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                loadResource(classes, classLoader, url);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load SPI file: " + fileName, e);
        }
        return classes;
    }

    private void loadResource(Map<String, Class<?>> classes,
                              ClassLoader classLoader,
                              URL resourceUrl) {

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String name;
                String className;

                int index = line.indexOf('=');
                if (index > 0) {
                    name = line.substring(0, index).trim();
                    className = line.substring(index + 1).trim();
                } else {
                    throw new IllegalStateException("Invalid SPI line: " + line);
                }

                Class<?> clazz = Class.forName(className, true, classLoader);
                classes.put(name, clazz);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load SPI resource " + resourceUrl, e);
        }
    }

    public List<T> getActivateExtensions(String group) {
        List<T> extensions = new ArrayList<>();

        for (Map.Entry<String, Class<?>> entry : getExtensionClasses().entrySet()) {
            Class<?> clazz = entry.getValue();
            Activate activate = clazz.getAnnotation(Activate.class);
            if (activate == null) {
                continue;
            }
            String[] scope = activate.scope();
            Set<String> set = new HashSet<>();
            set.addAll(Arrays.asList(scope));
            if (set.contains(group)) {
                extensions.add(getExtension(entry.getKey()));
            }
        }

        extensions.sort(Comparator.comparingInt(o ->
                o.getClass().getAnnotation(Activate.class).order()));

        return extensions;
    }

    private boolean isWrapperClass(Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            Class<?>[] params = constructor.getParameterTypes();
            if (params.length == 1 && params[0] == type) {
                return true;
            }
        }
        return false;
    }
}
