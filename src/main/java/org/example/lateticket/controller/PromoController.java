package org.example.lateticket.controller;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.PromotionalCampaign;
import org.example.lateticket.entity.User;
import org.example.lateticket.service.PromoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promo")
@RequiredArgsConstructor
public class PromoController {

    private final PromoService promoService;

    @PostMapping("/campaigns")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Map<String, Object>> createCampaign(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user) {
        Long eventId = Long.valueOf(body.get("eventId").toString());
        String name = (String) body.get("name");
        String discountType = (String) body.get("discountType");
        BigDecimal discountValue = new BigDecimal(body.get("discountValue").toString());
        Integer maxRedemptions = body.get("maxRedemptions") != null ?
                Integer.valueOf(body.get("maxRedemptions").toString()) : null;
        LocalDateTime validFrom = body.get("validFrom") != null ?
                LocalDateTime.parse(body.get("validFrom").toString()) : null;
        LocalDateTime validTo = body.get("validTo") != null ?
                LocalDateTime.parse(body.get("validTo").toString()) : null;

        return ResponseEntity.ok(promoService.createCampaign(
                eventId, name, discountType, discountValue, maxRedemptions, validFrom, validTo, user));
    }

    @GetMapping("/validate/{code}")
    public ResponseEntity<Map<String, Object>> validatePromoCode(@PathVariable String code) {
        return ResponseEntity.ok(promoService.validatePromoCode(code));
    }

    @GetMapping("/campaigns/event/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<List<PromotionalCampaign>> getEventCampaigns(@PathVariable Long eventId) {
        return ResponseEntity.ok(promoService.getEventCampaigns(eventId));
    }

    @PostMapping("/campaigns/{id}/deactivate")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> deactivateCampaign(
            @PathVariable Long id, @AuthenticationPrincipal User user) {
        promoService.deactivateCampaign(id, user);
        return ResponseEntity.ok().build();
    }
}