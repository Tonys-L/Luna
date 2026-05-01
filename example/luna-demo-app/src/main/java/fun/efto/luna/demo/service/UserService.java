package fun.efto.luna.demo.service;

import fun.efto.luna.demo.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author : Tony.L(<286269159@qq.com>)
 * @since : 2026/05/01 15:30
 */
public class UserService {
    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public User createUser(String name, int age) {
        long id = idGenerator.getAndIncrement();
        User user = new User(id, name, age, name.toLowerCase() + "@example.com");
        userStore.put(id, user);
        System.out.println("[UserService] Created user: " + user);
        return user;
    }

    public User getUser(Long id) {
        User user = userStore.get(id);
        if (user == null) {
            System.out.println("[UserService] User not found: id=" + id);
            return null;
        }
        System.out.println("[UserService] Found user: " + user);
        return user;
    }

    public User updateUser(Long id, String name, int age) {
        User user = userStore.get(id);
        if (user == null) {
            System.out.println("[UserService] User not found for update: id=" + id);
            return null;
        }
        user.setName(name);
        user.setAge(age);
        user.setEmail(name.toLowerCase() + "@example.com");
        System.out.println("[UserService] Updated user: " + user);
        return user;
    }

    public boolean deleteUser(Long id) {
        User removed = userStore.remove(id);
        if (removed == null) {
            System.out.println("[UserService] User not found for delete: id=" + id);
            return false;
        }
        System.out.println("[UserService] Deleted user: " + removed);
        return true;
    }

    public List<User> listUsers() {
        List<User> users = new ArrayList<>(userStore.values());
        System.out.println("[UserService] Listed " + users.size() + " users");
        return users;
    }

    public int getUserCount() {
        return userStore.size();
    }
}
