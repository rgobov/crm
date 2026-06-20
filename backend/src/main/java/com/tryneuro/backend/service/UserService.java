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

    public String getTenantIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getTenantId)
                .orElseThrow(() -> new RuntimeException("User or Tenant not found"));
    }

    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User login(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user.get();
        }
        return null;
    }
}
