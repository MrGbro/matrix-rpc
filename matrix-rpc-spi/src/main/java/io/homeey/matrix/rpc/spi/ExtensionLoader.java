package io.homeey.matrix.rpc.spi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ExtensionLoader<T> {
    private static final String SERVICES_DIR = "META-INF/services/";
    private static final String MATRIX_DIR = "META-INF/matrix/";

    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> LOADERS
            = new ConcurrentHashMap<>();

    private final Class<T> type;
    private final ConcurrentMap<String, Holder<Object>> cachedInstances
            = new ConcurrentHashMap<>();
    private volatile Class<?> cachedAdaptiveClass;
    private String cachedDefaultName;

    private ExtensionLoader(Class<T> type) {
        this.type = type;
    }

    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (!type.isInterface() || !type.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("Extension type must be SPI interface");
        }
        return (ExtensionLoader<T>) LOADERS.computeIfAbsent(type,
                k -> new ExtensionLoader<>(type));
    }

    // 获取指定名称的扩展实例
    public T getExtension(String name) {
        if ("adaptive".equals(name)) {
            return getAdaptiveExtension();
        }
        Holder<Object> holder = cachedInstances.computeIfAbsent(name, k -> new Holder<>());
        if (holder.get() == null) {
            synchronized (holder) {
                if (holder.get() == null) {
                    try {
                        holder.set(createExtension(name));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create extension " + name, e);
                    }
                }
            }
        }
        return (T) holder.get();
    }

    // 获取自适应扩展 (动态代理)
    public T getAdaptiveExtension() {
        if (cachedAdaptiveClass == null) {
            cachedAdaptiveClass = createAdaptiveExtensionClass();
        }
        return (T) cachedInstances.computeIfAbsent("adaptive",
                k -> new Holder<>()).get();
    }

    private <T> Class<T> createAdaptiveExtensionClass() {
        //todo
        return null;
    }

    // 获取默认扩展
    public T getDefaultExtension() {
        if (cachedDefaultName == null) {
            SPI spi = type.getAnnotation(SPI.class);
            cachedDefaultName = spi.value();
        }
        return getExtension(cachedDefaultName);
    }

    // 核心：创建扩展实例
    private T createExtension(String name) throws NoSuchMethodException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new IllegalStateException("No extension named " + name);
        }

        // 1. 实例化
        T instance = (T) clazz.getDeclaredConstructor().newInstance();

        // 2. 自动注入依赖 (简化版)
        injectDependencies(instance);

        // 3. 包装扩展 (Wrapper模式)
        instance = injectWrapper(instance);

        return instance;
    }

    // 加载扩展类
    private Map<String, Class<?>> getExtensionClasses() {
        // 从 META-INF/matrix 和 META-INF/services 加载
        Map<String, Class<?>> classes = new HashMap<>();
        loadDirectory(classes, MATRIX_DIR);
        loadDirectory(classes, SERVICES_DIR);
        return classes;
    }

    private void loadDirectory(Map<String, Class<?>> classes, String dir) {
        String fileName = dir + type.getName();
        try {
            Enumeration<URL> urls = getClassLoader().getResources(fileName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split("=");
                        String name = parts[0].trim();
                        String className = parts[1].trim();
                        classes.put(name, Class.forName(className));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load extension classes", e);
        }
    }

    // 简化版依赖注入
    private void injectDependencies(T instance) {
        //todo 实际项目中这里会实现 setter 注入
    }

    // Wrapper包装
    private T injectWrapper(T instance) {
        //todo 伪代码：扫描所有Wrapper类进行包装
        return instance;
    }

    private ClassLoader getClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }

    private static class Holder<T> {
        private volatile T value;

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }
}