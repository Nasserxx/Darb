package com.darb.dtos.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.darb.entities.enums.PaymentCycle;
import com.darb.entities.enums.PaymentMethod;
import com.darb.entities.enums.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment details response")
public class PaymentResponse {

    @Schema(description = "Unique payment identifier")
    private UUID id;

    @Schema(description = "Student ID")
    private UUID studentId;

    @Schema(description = "Circle ID")
    private UUID circleId;

    @Schema(description = "Mosque ID")
    private UUID mosqueId;

    @Schema(description = "Payment amount")
    private BigDecimal amount;

    @Schema(description = "Discount applied")
    private BigDecimal discount;

    @Schema(description = "Amount already paid")
    private BigDecimal amountPaid;

    @Schema(description = "Payment status", example = "PENDING")
    private PaymentStatus status;

    @Schema(description = "Payment method", example = "CASH")
    private PaymentMethod method;

    @Schema(description = "Billing cycle", example = "MONTHLY")
    private PaymentCycle cycle;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Date payment was made")
    private LocalDate paidDate;

    @Schema(description = "Receipt document URL")
    private String receiptUrl;

    @Schema(description = "User ID who recorded the payment")
    private UUID recordedBy;

    @Schema(description = "Additional notes")
    private String notes;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;
}
