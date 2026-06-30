package com.tryneuro.backend.service;

import com.tryneuro.backend.dto.ConsentDto;
import com.tryneuro.backend.model.UserConsent;
import com.tryneuro.backend.repository.UserConsentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConsentService {

    private final UserConsentRepository userConsentRepository;

    public ConsentService(UserConsentRepository userConsentRepository) {
        this.userConsentRepository = userConsentRepository;
    }

    @Transactional
    public UserConsent saveConsent(String userId, String consentType, String policyVersion, String ipAddress, String userAgent) {
        UserConsent consent = new UserConsent();
        consent.setUserId(userId);
        consent.setConsentType(consentType);
        consent.setPolicyVersion(policyVersion);
        consent.setIpAddress(ipAddress);
        consent.setUserAgent(userAgent);
        consent.setAcceptedAt(LocalDateTime.now());
        return userConsentRepository.save(consent);
    }

    @Transactional
    public void revokeConsent(String consentId, String userId) {
        UserConsent consent = userConsentRepository.findById(consentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Согласие не найдено"));
        if (!consent.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Это не ваше согласие");
        }
        if (consent.getRevokedAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Согласие уже отозвано");
        }
        consent.setRevokedAt(LocalDateTime.now());
        userConsentRepository.save(consent);
    }

    public List<ConsentDto> getActiveConsents(String userId) {
        return userConsentRepository.findByUserIdAndRevokedAtIsNull(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ConsentDto> getAllConsents(String userId) {
        return userConsentRepository.findByUserId(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private ConsentDto toDto(UserConsent c) {
        return new ConsentDto(
                c.getId(),
                c.getConsentType(),
                c.getPolicyVersion(),
                c.getIpAddress(),
                c.getAcceptedAt(),
                c.getRevokedAt()
        );
    }
}
