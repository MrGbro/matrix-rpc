package io.homeey.example.api;

/**
 * EchoService 接口，用于测试服务调用
 *
 * @author jt4mrg@gmail.com
 * @since 2025/01/10
 */
public interface EchoService {
    String echo(String msg);

    /**
     * 测试复杂对象传递
     */
    User getUser(Long id);

    /**
     * 测试复杂对象作为参数和返回值
     */
    User saveUser(User user);
}
