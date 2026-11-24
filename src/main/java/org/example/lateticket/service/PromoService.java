package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.*;
import org.example.lateticket.entity.enums.DiscountType;
import org.example.lateticket.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromoService {

    private final PromotionalCampaignRepository campaignRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final EventRepository eventRepository;
    private final AuditService auditService;

    public Map<String, Object> createCampaign(Long eventId, String name, String discountType,
                                               BigDecimal discountValue, Integer maxRedemptions,
                                               LocalDateTime validFrom, LocalDateTime validTo,
                                               User organizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }

        PromotionalCampaign campaign = PromotionalCampaign.builder()
                .event(event)
                .name(name)
                .discountType(DiscountType.valueOf(discountType.toUpperCase()))
                .discountValue(discountValue)
                .maxRedemptions(maxRedemptions)
                .currentRedemptions(0)
                .validFrom(validFrom)
                .validTo(validTo)
                .active(true)
                .build();
        campaign = campaignRepository.save(campaign);

        // Generate promo code
        String code = "BF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PromoCode promoCode = PromoCode.builder()
                .campaign(campaign)
                .code(code)
                .redemptions(0)
                .build();
        promoCodeRepository.save(promoCode);

        // Generate campaign QR link (opaque token, not discount value)
        String campaignToken = UUID.randomUUID().toString();
        String campaignLink = "/events/" + eventId + "?promo=" + code;

        auditService.log(eventId, organizer.getId(), "CREATE", "Campaign", campaign.getId(),
                "Campaign created: " + name);

        return Map.of(
                "campaignId", campaign.getId(),
                "promoCode", code,
                "campaignLink", campaignLink,
                "discountType", campaign.getDiscountType().name(),
                "discountValue", campaign.getDiscountValue()
        );
    }

    public Map<String, Object> validatePromoCode(String code) {
        PromoCode promoCode = promoCodeRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Invalid promo code"));

        PromotionalCampaign campaign = promoCode.getCampaign();

        if (!campaign.isActive()) {
            throw new RuntimeException("Promo code is disabled");
        }
        if (campaign.getValidTo() != null && LocalDateTime.now().isAfter(campaign.getValidTo())) {
            throw new RuntimeException("Promo code has expired");
        }
        if (campaign.getMaxRedemptions() != null &&
                campaign.getCurrentRedemptions() >= campaign.getMaxRedemptions()) {
            throw new RuntimeException("Promo code has reached its redemption limit");
        }

        return Map.of(
                "valid", true,
                "discountType", campaign.getDiscountType().name(),
                "discountValue", campaign.getDiscountValue(),
                "eventId", campaign.getEvent().getId()
        );
    }

    public List<PromotionalCampaign> getEventCampaigns(Long eventId, User organizer) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }

        List<PromotionalCampaign> campaigns = campaignRepository.findByEventId(eventId);
        if (campaigns.isEmpty()) return campaigns;

        Map<Long, String> codes = promoCodeRepository
                .findByCampaignIdIn(campaigns.stream().map(PromotionalCampaign::getId).toList())
                .stream()
                .collect(Collectors.toMap(pc -> pc.getCampaign().getId(), PromoCode::getCode, (a, b) -> a));
        campaigns.forEach(c -> c.setPromoCode(codes.get(c.getId())));
        return campaigns;
    }

    public void deactivateCampaign(Long campaignId, User organizer) {
        PromotionalCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));
        if (!campaign.getEvent().getOrganizer().getId().equals(organizer.getId())) {
            throw new RuntimeException("Not authorized");
        }
        campaign.setActive(false);
        campaignRepository.save(campaign);
    }
}