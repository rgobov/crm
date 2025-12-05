package com.tryneuro.backend.service;

import com.tryneuro.backend.model.User;
import com.tryneuro.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers(String tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    public User createUser(User user) {
        // tenantId должен быть установлен в контроллере
        return userRepository.save(user);
    }

    public User login(String email, String password) {
        // В реальном приложении здесь должна быть проверка хеша пароля
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get();
        }
        return null;
    }
}
