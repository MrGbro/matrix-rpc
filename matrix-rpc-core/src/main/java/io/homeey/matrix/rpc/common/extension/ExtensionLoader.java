package io.homeey.matrix.rpc.common.extension;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扩展点加载器 - 微内核核心
 * 实现类似Dubbo的SPI机制，支持：
 * 1. 按需加载扩展实现
 * 2. 扩展点自动注入
 * 3. 自适应扩展
 * 4. 扩展点包装(AOP)
 */
public class ExtensionLoader<T> {

    private static final String EXTENSION_PATH = "META-INF/extension/";

    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    private final Class<T> type;
    private final Map<String, Class<?>> extensionClasses = new ConcurrentHashMap<>();
    private final Map<String, T> cachedInstances = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;
    private String defaultName;

    private ExtensionLoader(Class<T> type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an interface!");
        }
        if (!type.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not annotated with @SPI!");
        }

        return (ExtensionLoader<T>) EXTENSION_LOADERS.computeIfAbsent(type, ExtensionLoader::new);
    }

    public T getExtension(String name) {
        if (name == null || name.isEmpty()) {
            name = getDefaultExtensionName();
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("No default extension for " + type.getName());
        }

        final String extensionName = name;
        return cachedInstances.computeIfAbsent(extensionName, this::createExtension);
    }

    public T getDefaultExtension() {
        return getExtension(getDefaultExtensionName());
    }

    public Set<String> getSupportedExtensions() {
        loadExtensionClasses();
        return Collections.unmodifiableSet(extensionClasses.keySet());
    }

    private String getDefaultExtensionName() {
        if (defaultName == null) {
            SPI spi = type.getAnnotation(SPI.class);
            if (spi != null && !spi.value().isEmpty()) {
                defaultName = spi.value();
            }
        }
        return defaultName;
    }

    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        loadExtensionClasses();
        Class<?> clazz = extensionClasses.get(name);
        if (clazz == null) {
            throw new IllegalStateException("No extension named '" + name + "' for " + type.getName());
        }

        try {
            T instance = (T) EXTENSION_INSTANCES.computeIfAbsent(clazz, c -> {
                try {
                    return c.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create extension instance", e);
                }
            });
            injectExtension(instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create extension " + name, e);
        }
    }

    private void injectExtension(T instance) {
        // TODO: 实现扩展点自动注入
    }

    private void loadExtensionClasses() {
        if (loaded) return;
        synchronized (extensionClasses) {
            if (loaded) return;

            String fileName = EXTENSION_PATH + type.getName();
            try {
                Enumeration<URL> urls = ClassLoader.getSystemResources(fileName);
                if (urls != null) {
                    while (urls.hasMoreElements()) {
                        loadResource(urls.nextElement());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load extension classes", e);
            }
            loaded = true;
        }
    }

    private void loadResource(URL url) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int commentIndex = line.indexOf('#');
                if (commentIndex >= 0) line = line.substring(0, commentIndex);
                line = line.trim();
                if (line.isEmpty()) continue;

                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    String name = line.substring(0, eqIndex).trim();
                    String className = line.substring(eqIndex + 1).trim();
                    if (!name.isEmpty() && !className.isEmpty()) {
                        Class<?> clazz = Class.forName(className, true, ClassLoader.getSystemClassLoader());
                        if (!type.isAssignableFrom(clazz)) {
                            throw new IllegalStateException(clazz.getName() + " is not subtype of " + type.getName());
                        }
                        extensionClasses.put(name, clazz);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load extension from " + url, e);
        }
    }
}
