package org.example.lateticket.dto.seat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Describes the hall layout. Each section is bound to one ticket type (price tier).
 * Rows can be given explicitly ({@code rows}) or generated from {@code rowCount} x {@code seatsPerRow}
 * (rows labelled 1..rowCount, seats numbered 1..seatsPerRow).
 */
@Data
public class SeatMapRequest {

    @NotEmpty @Valid
    private List<SectionRequest> sections;

    @Data
    public static class SectionRequest {
        @NotBlank
        private String name;

        @NotNull
        private Long ticketTypeId;

        @Valid
        private List<RowRequest> rows;

        @Min(1)
        private Integer rowCount;

        @Min(1)
        private Integer seatsPerRow;
    }

    @Data
    public static class RowRequest {
        @NotBlank
        private String label;

        @NotNull @Min(1)
        private Integer seats;
    }
}
