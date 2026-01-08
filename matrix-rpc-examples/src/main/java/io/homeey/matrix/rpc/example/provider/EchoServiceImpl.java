package io.homeey.matrix.rpc.example.provider;

import io.homeey.matrix.rpc.example.api.EchoService;
import io.homeey.matrix.rpc.example.api.User;

public class EchoServiceImpl implements EchoService {

    @Override
    public String echo(String msg) {
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
