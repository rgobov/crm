package com.tryneuro.backend.repository;

import com.tryneuro.backend.model.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, String> {

    List<UserConsent> findByUserIdAndRevokedAtIsNull(String userId);

    List<UserConsent> findByUserId(String userId);
}
