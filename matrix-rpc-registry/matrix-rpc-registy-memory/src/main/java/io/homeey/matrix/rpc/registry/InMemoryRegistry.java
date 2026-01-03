//package io.homeey.matrix.rpc.registry;
//
//
//import io.homeey.matrix.rpc.registry.api.NotifyListener;
//import io.homeey.matrix.rpc.registry.api.Registry;
//import io.homeey.matrix.rpc.registry.api.ServiceInstance;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.CopyOnWriteArrayList;
//
//public class InMemoryRegistry implements Registry {
//
//    private final Map<String, List<ServiceInstance>> services = new ConcurrentHashMap<>();
//    private final Map<String, List<NotifyListener>> listeners = new ConcurrentHashMap<>();
//
//    @Override
//    public void register(ServiceInstance instance) {
//        services
//                .computeIfAbsent(instance.getServiceName(), k -> new CopyOnWriteArrayList<>())
//                .add(instance);
//        notifyListeners(instance.getServiceName());
//    }
//
//    @Override
//    public void unregister(ServiceInstance instance) {
//        List<ServiceInstance> list = services.get(instance.getServiceName());
//        if (list != null) {
//            list.removeIf(i ->
//                    i.getHost().equals(instance.getHost())
//                            && i.getPort() == instance.getPort()
//            );
//            notifyListeners(instance.getServiceName());
//        }
//    }
//
//    @Override
//    public void subscribe(String serviceName, NotifyListener listener) {
//        listeners
//                .computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>())
//                .add(listener);
//    }
//
//    @Override
//    public void unsubscribe(String serviceName, NotifyListener listener) {
//        List<NotifyListener> list = listeners.get(serviceName);
//        if (list != null) {
//            list.remove(listener);
//        }
//    }
//
//    private void notifyListeners(String serviceName) {
//        List<NotifyListener> list = listeners.get(serviceName);
//        if (list != null) {
//            List<ServiceInstance> instances =
//                    services.getOrDefault(serviceName, Collections.emptyList());
//            for (NotifyListener l : list) {
//                l.notify(instances);
//            }
//        }
//    }
//}
