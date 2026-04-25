package com.darb.dtos.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.PaymentCycle;
import com.darb.entities.enums.PaymentMethod;
import com.darb.entities.enums.PaymentStatus;

@Data
@Schema(description = "Request body for creating a payment")
public class PaymentCreateRequest {

    @NotNull
    @Schema(description = "Student ID", example = "c3d4e5f6-a7b8-9012-cdef-123456789012")
    private UUID studentId;

    @NotNull
    @Schema(description = "Circle ID")
    private UUID circleId;

    @NotNull
    @Schema(description = "Mosque ID")
    private UUID mosqueId;

    @NotNull
    @Schema(description = "Payment amount", example = "150.00")
    private BigDecimal amount;

    @Schema(description = "Discount applied", example = "15.00")
    private BigDecimal discount;

    @Schema(description = "Amount already paid", example = "0.00")
    private BigDecimal amountPaid;

    @Schema(description = "Payment status", example = "PENDING")
    private PaymentStatus status;

    @Schema(description = "Payment method", example = "CASH")
    private PaymentMethod method;

    @Schema(description = "Billing cycle", example = "MONTHLY")
    private PaymentCycle cycle;

    @NotNull
    @Schema(description = "Due date", example = "2026-05-01")
    private LocalDate dueDate;

    @Schema(description = "Date payment was made")
    private LocalDate paidDate;

    @Schema(description = "Receipt document URL")
    private String receiptUrl;

    @Schema(description = "User ID who recorded the payment")
    private UUID recordedBy;

    @Schema(description = "Additional notes")
    private String notes;
}
