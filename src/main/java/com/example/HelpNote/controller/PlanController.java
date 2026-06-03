package com.example.HelpNote.controller;

import com.example.HelpNote.dto.UsageStatusResponse;
import com.example.HelpNote.service.UsageLimitService;
import com.example.HelpNote.util.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final UsageLimitService usageLimitService;

    public PlanController(UsageLimitService usageLimitService) {
        this.usageLimitService = usageLimitService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getPlanStatus(HttpServletRequest request) {
        Long userId = AuthUtils.getUserId(request);
        if (userId == null) return AuthUtils.unauthorized();

        UsageStatusResponse status = usageLimitService.getUsageStatus(userId);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradeToPremium(HttpServletRequest request) {
        Long userId = AuthUtils.getUserId(request);
        if (userId == null) return AuthUtils.unauthorized();

        usageLimitService.upgradeToPremium(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Plano atualizado para Premium com sucesso!");
        response.put("planType", "PREMIUM");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/downgrade")
    public ResponseEntity<?> downgradeToFree(HttpServletRequest request) {
        Long userId = AuthUtils.getUserId(request);
        if (userId == null) return AuthUtils.unauthorized();

        usageLimitService.downgradeToFree(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Plano alterado para Gratuito.");
        response.put("planType", "FREE");
        return ResponseEntity.ok(response);
    }
}
