package org.example.lateticket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cancels PENDING orders that were never paid, so their seats and tickets go back on sale. */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryJob {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.orders.expiry-check-ms:60000}")
    public void expireStalePendingOrders() {
        for (Long orderId : orderService.findStalePendingOrderIds()) {
            try {
                if (orderService.expireIfStale(orderId)) {
                    log.info("Expired unpaid order {}", orderId);
                }
            } catch (RuntimeException e) {
                log.warn("Failed to expire order {}", orderId, e);
            }
        }
    }
}
