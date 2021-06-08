package com.ippon.bankapp.domain;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "type")
    private String type;

    @Column(name = "amount")
    private BigDecimal amount;

    private LocalDate date;

    @NotNull
    @ManyToOne
    private Account account;

    public Transaction() {}

    public Transaction(Account account, String type, BigDecimal amount) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        date = LocalDate.now();
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public LocalDate getDate() {
        return date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction transaction = (Transaction) o;
        return getId() == transaction.getId() &&
                Objects.equals(getType(), transaction.getType()) &&
                getAmount().equals(transaction.getAmount());

    }

    @Override
    public int hashCode() { return Objects.hash(getId(), getType(), getAmount());}

    @Override
    public String toString() {
        return "Transaction{" +
                "accountID=" + id +
                ", type=" + type + '\'' +
                ", amount=" + amount.toString() + '\'' +
                '}';
    }
}
