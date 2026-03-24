package com.example.HelpNote.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.HelpNote.domain.PlanType;
import com.example.HelpNote.domain.User;
import com.example.HelpNote.dto.UsageStatusResponse;
import com.example.HelpNote.repository.UserRepository;

@Service
public class UsageLimitService {

    private static final int FREE_DAILY_NOTE_LIMIT = 1;
    private static final int FREE_DAILY_ATA_LIMIT = 1;
    private static final int PREMIUM_UNLIMITED = -1; // -1 represents unlimited

    private final UserRepository userRepository;

    public UsageLimitService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resets daily counters if the date has changed since last reset.
     */
    private void resetDailyCountersIfNeeded(User user) {
        LocalDate today = LocalDate.now();
        if (user.getLastUsageResetDate() == null || !user.getLastUsageResetDate().equals(today)) {
            user.setDailyNoteCount(0);
            user.setDailyAtaCount(0);
            user.setLastUsageResetDate(today);
            userRepository.save(user);
        }
    }

    /**
     * Check if a user can create a new smart note.
     */
    public boolean canCreateNote(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getPlanType() == PlanType.PREMIUM) {
            return true;
        }

        resetDailyCountersIfNeeded(user);
        return user.getDailyNoteCount() < FREE_DAILY_NOTE_LIMIT;
    }

    /**
     * Check if a user can create a new ata (meeting minutes).
     */
    public boolean canCreateAta(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getPlanType() == PlanType.PREMIUM) {
            return true;
        }

        resetDailyCountersIfNeeded(user);
        return user.getDailyAtaCount() < FREE_DAILY_ATA_LIMIT;
    }

    /**
     * Increment the daily note usage counter.
     */
    public void incrementNoteUsage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        resetDailyCountersIfNeeded(user);
        user.setDailyNoteCount(user.getDailyNoteCount() + 1);
        userRepository.save(user);
    }

    /**
     * Increment the daily ata usage counter.
     */
    public void incrementAtaUsage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        resetDailyCountersIfNeeded(user);
        user.setDailyAtaCount(user.getDailyAtaCount() + 1);
        userRepository.save(user);
    }

    /**
     * Upgrade user to Premium plan.
     */
    public void upgradeToPremium(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setPlanType(PlanType.PREMIUM);
        user.setPlanStartDate(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Downgrade user to Free plan.
     */
    public void downgradeToFree(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setPlanType(PlanType.FREE);
        user.setPlanStartDate(null);
        userRepository.save(user);
    }

    /**
     * Get the current usage status for a user.
     */
    public UsageStatusResponse getUsageStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        resetDailyCountersIfNeeded(user);

        boolean isPremium = user.getPlanType() == PlanType.PREMIUM;

        int maxNotes = isPremium ? PREMIUM_UNLIMITED : FREE_DAILY_NOTE_LIMIT;
        int maxAtas = isPremium ? PREMIUM_UNLIMITED : FREE_DAILY_ATA_LIMIT;

        int remainingNotes = isPremium ? PREMIUM_UNLIMITED : Math.max(0, FREE_DAILY_NOTE_LIMIT - user.getDailyNoteCount());
        int remainingAtas = isPremium ? PREMIUM_UNLIMITED : Math.max(0, FREE_DAILY_ATA_LIMIT - user.getDailyAtaCount());

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
