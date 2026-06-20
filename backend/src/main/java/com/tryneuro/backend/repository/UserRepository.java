package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByTenantId(String tenantId);
    Optional<User> findByStaffId(String staffId);
    
    // Поиск пользователя по Telegram ID для авто-входа
    Optional<User> findByTelegramId(Long telegramId);
    
    // Поиск пользователя по Contact ID
    Optional<User> findByContactId(String contactId);
}
