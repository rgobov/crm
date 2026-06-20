package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.UserAiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAiConfigRepository extends JpaRepository<UserAiConfig, String> {
}