package com.example.HelpNote.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.HelpNote.domain.PlanType;
import com.example.HelpNote.domain.User;
import com.example.HelpNote.dto.UsageStatusResponse;
import com.example.HelpNote.repository.UserRepository;

@Service
public class UsageLimitService {

    private static final Logger log = LoggerFactory.getLogger(UsageLimitService.class);

    private static final int FREE_DAILY_NOTE_LIMIT = 3;
    private static final int FREE_DAILY_ATA_LIMIT = 2;

    private final UserRepository userRepository;

    public UsageLimitService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private void resetDailyCountersIfNeeded(User user) {
        LocalDate today = LocalDate.now();
        if (user.getLastUsageResetDate() == null || !user.getLastUsageResetDate().equals(today)) {
            user.setDailyNoteCount(0);
            user.setDailyAtaCount(0);
            user.setLastUsageResetDate(today);
        }
    }

    @Transactional
    public boolean checkAndIncrementNote(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getPlanType() == PlanType.PREMIUM) return true;

        resetDailyCountersIfNeeded(user);

        if (user.getDailyNoteCount() >= FREE_DAILY_NOTE_LIMIT) return false;

        user.setDailyNoteCount(user.getDailyNoteCount() + 1);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public boolean checkAndIncrementAta(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getPlanType() == PlanType.PREMIUM) return true;

        resetDailyCountersIfNeeded(user);

        if (user.getDailyAtaCount() >= FREE_DAILY_ATA_LIMIT) return false;

        user.setDailyAtaCount(user.getDailyAtaCount() + 1);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public void upgradeToPremium(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        user.setPlanType(PlanType.PREMIUM);
        user.setPlanStartDate(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void downgradeToFree(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        user.setPlanType(PlanType.FREE);
        user.setPlanStartDate(null);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UsageStatusResponse getUsageStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        boolean isPremium = user.getPlanType() == PlanType.PREMIUM;
        LocalDate today = LocalDate.now();
        int noteCount = (user.getLastUsageResetDate() != null && user.getLastUsageResetDate().equals(today))
                ? user.getDailyNoteCount() : 0;
        int ataCount = (user.getLastUsageResetDate() != null && user.getLastUsageResetDate().equals(today))
                ? user.getDailyAtaCount() : 0;

        int maxNotes = isPremium ? -1 : FREE_DAILY_NOTE_LIMIT;
        int maxAtas = isPremium ? -1 : FREE_DAILY_ATA_LIMIT;
        int remainingNotes = isPremium ? -1 : Math.max(0, FREE_DAILY_NOTE_LIMIT - noteCount);
        int remainingAtas = isPremium ? -1 : Math.max(0, FREE_DAILY_ATA_LIMIT - ataCount);

        return new UsageStatusResponse(
                user.getPlanType().name(),
                remainingNotes,
                remainingAtas,
                maxNotes,
                maxAtas,
                isPremium
        );
    }
}
