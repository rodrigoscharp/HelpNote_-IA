package com.example.HelpNote.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.HelpNote.dto.UsageStatusResponse;
import com.example.HelpNote.service.UsageLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final UsageLimitService usageLimitService;

    public PlanController(UsageLimitService usageLimitService) {
        this.usageLimitService = usageLimitService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getPlanStatus(HttpServletRequest request) {
        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Não autenticado");
        }

        UsageStatusResponse status = usageLimitService.getUsageStatus(userId);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradeToPremium(HttpServletRequest request) {
        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Não autenticado");
        }

        usageLimitService.upgradeToPremium(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Plano atualizado para Premium com sucesso!");
        response.put("planType", "PREMIUM");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/downgrade")
    public ResponseEntity<?> downgradeToFree(HttpServletRequest request) {
        Long userId = getUserIdFromSession(request);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Não autenticado");
        }

        usageLimitService.downgradeToFree(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Plano alterado para Gratuito.");
        response.put("planType", "FREE");
        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return (Long) session.getAttribute("userId");
        }
        return null;
    }
}
