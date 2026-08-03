package fun.efto.luna.demo.repository;

import fun.efto.luna.demo.model.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/07/05 19:10
 */
public class UserRepository {
    private final Map<Long, User> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public User save(User user) {
        simulateDelay(2);
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        store.put(user.getId(), user);
        return user;
    }

    public User findById(Long id) {
        simulateDelay(1);
        return store.get(id);
    }

    public boolean existsByName(String name) {
        simulateDelay(1);
        for (User user : store.values()) {
            if (user.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public User deleteById(Long id) {
        simulateDelay(1);
        return store.remove(id);
    }

    public int count() {
        return store.size();
    }

    private void simulateDelay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
