package io.homeey.example.provider;

import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.example.api.User;

import java.util.concurrent.atomic.AtomicInteger;

public class EchoServiceImpl implements EchoService {
    
    // 用于模拟异常的计数器
    private static final AtomicInteger callCounter = new AtomicInteger(0);
    // 控制异常率: 每N次调用抛出一次异常
    private static final int ERROR_FREQUENCY = 2; // 50%异常率

    @Override
    public String echo(String msg) {
        // 支持特殊消息触发异常 (用于熔断测试)
        if (msg != null && msg.startsWith("ERROR-")) {
            int count = callCounter.incrementAndGet();
            if (count % ERROR_FREQUENCY == 0) {
                System.err.println("[Provider] Simulating error for: " + msg);
                throw new RuntimeException("Simulated error for circuit breaker test");
            }
        }
        return "echo: " + msg;
    }

    @Override
    public User getUser(Long id) {
        // 模拟查询用户
        return new User(id, "User-" + id, "user" + id + "@example.com", 25);
    }

    @Override
    public User saveUser(User user) {
        // 模拟保存用户，并返回带ID的用户
        if (user.getId() == null) {
            user.setId(System.currentTimeMillis());
        }
        System.out.println("[Provider] Saved user: " + user);
        return user;
    }
}
