package com.darb.dtos.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.darb.entities.enums.PaymentMethod;
import com.darb.entities.enums.PaymentStatus;

@Data
@Schema(description = "Request body for updating a payment")
public class PaymentUpdateRequest {

    @Schema(description = "Discount applied")
    private BigDecimal discount;

    @Schema(description = "Amount already paid")
    private BigDecimal amountPaid;

    @Schema(description = "Payment status")
    private PaymentStatus status;

    @Schema(description = "Payment method")
    private PaymentMethod method;

    @Schema(description = "Due date")
    private LocalDate dueDate;

    @Schema(description = "Date payment was made")
    private LocalDate paidDate;

    @Schema(description = "Receipt document URL")
    private String receiptUrl;

    @Schema(description = "Additional notes")
    private String notes;
}
