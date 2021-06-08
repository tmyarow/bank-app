package com.ippon.bankapp.rest;

import com.ippon.bankapp.domain.Transaction;
import com.ippon.bankapp.service.AccountService;
import com.ippon.bankapp.service.dto.AccountDTO;
import com.ippon.bankapp.service.dto.AmountDTO;
import com.ippon.bankapp.service.dto.TransactionDTO;
import com.ippon.bankapp.service.dto.TransferDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/account")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDTO createAccount(@Valid @RequestBody AccountDTO newAccount) {
        return accountService.createAccount(newAccount);
    }

    @GetMapping("/account/{lastName}")
    public AccountDTO account(@PathVariable(name = "lastName") String lastName) {
        return accountService.getAccountDTOByLastName(lastName);
    }

    /**
     * Finds the account with first name given as a path variable in api call and returns it's DTO
     *
     * @param firstName    The first name of the account to find
     * @return             AccountDTO containing information for account found
     */
    @GetMapping("/account/first/{firstName}")
    public AccountDTO accountFirstName(@PathVariable(name = "firstName") String firstName) {
        return accountService.getAccountDTOByFirstName(firstName);
    }

    /**
     * Deposit an amount specified in the request body into an account specified by path variable
     *
     * @param lastName    The last name of the account to deposit into
     * @param amount      The amount to deposit into the account
     * @return            AccountDTO containing updated information for account deposited into
     */
    @PostMapping("/account/deposit/{lastName}")
    public AccountDTO deposit(@PathVariable String lastName, @Valid @RequestBody AmountDTO amount) {
        return accountService.deposit(lastName, amount.getAmount());
    }

    /**
     * Withdraw an amount specified in the request body into an account specified by path variable
     *
     * @param lastName    The last name of the account to deposit into
     * @param amount      A DTO amount to withdraw from the account
     * @return            AccountDTO containing updated information for account withdrawn from
     */
    @PostMapping("/account/withdraw/{lastName}")
    public AccountDTO withdraw(@PathVariable String lastName, @Valid @RequestBody AmountDTO amount) {
        return accountService.withdraw(lastName, amount.getAmount());
    }

    /**
     * Transfers a specified amount from one account to another. Two accounts and amount are given in request body
     *
     * @param transfer A DTO containing accounts to transfer between and amount to transfer
     */
    @PostMapping("/account/transfer")
    public void transfer(@Valid @RequestBody TransferDTO transfer) {
        accountService.transfer(transfer.getFrom(), transfer.getTo(), transfer.getAmount());
        return;
    }

    @GetMapping("/account/transactions/{lastName}")
    public List<TransactionDTO> getLatestTransaction(@PathVariable String lastName) {
        return accountService.getLatestTenTransaction(lastName);
    }
}