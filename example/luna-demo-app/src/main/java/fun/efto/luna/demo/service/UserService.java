package fun.efto.luna.demo.service;

import fun.efto.luna.demo.model.User;
import fun.efto.luna.demo.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Tony.L(286269159@qq.com)
 * @since : 2026/05/01 15:30
 */
public class UserService {
    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User createUser(String name, int age) {
        if (userRepo.existsByName(name)) {
            System.out.println("[UserService] User already exists: " + name);
            return null;
        }
        User user = new User(null, name, age, name.toLowerCase() + "@example.com");
        userRepo.save(user);
        System.out.println("[UserService] Created user: " + user);
        return user;
    }

    public User getUser(Long id) {
        User user = userRepo.findById(id);
        if (user == null) {
            System.out.println("[UserService] User not found: id=" + id);
        } else {
            System.out.println("[UserService] Found user: " + user);
        }
        return user;
    }

    public User updateUser(Long id, String name, int age) {
        User user = userRepo.findById(id);
        if (user == null) {
            System.out.println("[UserService] User not found for update: id=" + id);
            return null;
        }
        user.setName(name);
        user.setAge(age);
        user.setEmail(name.toLowerCase() + "@example.com");
        userRepo.save(user);
        System.out.println("[UserService] Updated user: " + user);
        return user;
    }

    public boolean deleteUser(Long id) {
        User removed = userRepo.deleteById(id);
        if (removed == null) {
            System.out.println("[UserService] User not found for delete: id=" + id);
            return false;
        }
        System.out.println("[UserService] Deleted user: " + removed);
        return true;
    }

    public int getUserCount() {
        return userRepo.count();
    }
}
