package org.example.lateticket.repository;

import org.example.lateticket.entity.PromotionalCampaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionalCampaignRepository extends JpaRepository<PromotionalCampaign, Long> {
    List<PromotionalCampaign> findByEventId(Long eventId);
    List<PromotionalCampaign> findByEventIdAndActiveTrue(Long eventId);
}