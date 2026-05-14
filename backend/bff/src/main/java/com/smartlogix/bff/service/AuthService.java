package com.smartlogix.bff.service;

import com.smartlogix.bff.model.User;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    private Map<String, User> users = new HashMap<>();

    public AuthService() {
        // Usuarios por defecto
        users.put("admin", new User("admin", "admin123", "ADMIN"));
        users.put("user1", new User("user1", "password123", "USER"));
        users.put("user2", new User("user2", "password456", "USER"));
    }

    public User authenticate(String username, String password) {
        User user = users.get(username);
        
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        
        return null;
    }

    public void registerUser(String username, String password, String role) {
        if (!users.containsKey(username)) {
            users.put(username, new User(username, password, role));
        }
    }

    public User getUserByUsername(String username) {
        return users.get(username);
    }
}
