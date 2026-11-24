package org.example.lateticket.repository;

import org.example.lateticket.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCode(String code);

    List<PromoCode> findByCampaignIdIn(Collection<Long> campaignIds);
}