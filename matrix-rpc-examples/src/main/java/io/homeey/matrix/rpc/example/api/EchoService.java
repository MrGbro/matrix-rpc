package io.homeey.matrix.rpc.example.api;

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
