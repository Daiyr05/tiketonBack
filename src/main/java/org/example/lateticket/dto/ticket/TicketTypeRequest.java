package org.example.lateticket.dto.ticket;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TicketTypeRequest {
    @NotBlank
    private String name;
    private String description;

    @NotNull @Min(0)
    private BigDecimal price;

    @NotNull @Min(1)
    private Integer quantity;

    private boolean free;
    private LocalDateTime salesStart;
    private LocalDateTime salesEnd;
    private Integer maxPerOrder;
}