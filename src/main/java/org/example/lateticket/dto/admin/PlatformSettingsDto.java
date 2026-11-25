package org.example.lateticket.dto.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSettingsDto {
    /** Percent of the order subtotal after discounts, 0-100 */
    @NotNull @DecimalMin("0") @DecimalMax("100")
    private BigDecimal serviceFeePercent;
    /** Fixed KZT amount added to each paid order */
    @NotNull @DecimalMin("0")
    private BigDecimal serviceFeeFixed;
    private LocalDateTime updatedAt;
}
