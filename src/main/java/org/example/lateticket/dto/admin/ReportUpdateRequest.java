package org.example.lateticket.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportUpdateRequest {
    /** OPEN, RESOLVED or DISMISSED */
    @NotBlank
    private String status;
    private String adminNote;
}
