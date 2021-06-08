package com.ippon.bankapp.service.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * A DTO to hold information for a transfer between two accounts
 */
public class TransferDTO {

    public TransferDTO() {
    }

    /**
     * The last name of the account which to transfer from
     */
    @NotNull
    private String from;

    /**
     * The last name of the account which to transfer to
     */
    @NotNull
    private String to;

    /**
     * The amount to be transferred
     */
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
