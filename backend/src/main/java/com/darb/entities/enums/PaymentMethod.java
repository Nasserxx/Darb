package com.darb.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Method used to make a payment.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CASH("cash", "Cash payment"),
    BANK_TRANSFER("bank_transfer", "Bank transfer"),
    CARD("card", "Credit/debit card"),
    ONLINE("online", "Online payment gateway"),
    OTHER("other", "Other payment method");

    private final String key;
    private final String description;
}
