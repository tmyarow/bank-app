package com.ippon.bankapp.service;

import com.ippon.bankapp.domain.Account;
import com.ippon.bankapp.domain.Transaction;
import com.ippon.bankapp.repository.AccountRepository;
import com.ippon.bankapp.repository.TransactionRepository;
import com.ippon.bankapp.service.dto.AccountDTO;
import com.ippon.bankapp.service.dto.TransactionDTO;
import com.ippon.bankapp.service.exception.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccountService {

    private AccountRepository accountRepository;
    private NotificationFactory notificationFactory;
    private TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, NotificationFactory notificationFactory, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.notificationFactory = notificationFactory;
        this.transactionRepository = transactionRepository;
    }

    public AccountDTO createAccount(AccountDTO newAccount) {
        validateLastNameUnique(newAccount.getLastName());
        Account account = new Account(newAccount.getFirstName(), newAccount.getLastName());
        account.setNotificationPreference(notificationFactory
                .getDefaultNotification()
                .getName());

        Account save = accountRepository.save(account);

        notificationFactory
                .getPreferredService(save.getNotificationPreference())
                .orElseGet(notificationFactory::getDefaultNotification)
                .sendMessage("bank",
                        account.getLastName(),
                        "Account Created",
                        "Welcome aboard!");

        return mapAccountToDTO(save);
    }

    public AccountDTO getAccountDTOByLastName(String lastName) {
        Account account = accountRepository
                .findByLastName(lastName)
                .orElseThrow(AccountNotFoundException::new);

        return mapAccountToDTO(account);
    }

    /**
     * Find an account from a first name
     *
     * @param firstName first name of account to find
     * @return          DTO containing information on account found
     */
    public AccountDTO getAccountDTOByFirstName(String firstName) {
        Account account = accountRepository
                .findByFirstName(firstName)
                .orElseThrow(AccountNotFoundException::new);

        return mapAccountToDTO(account);
    }

    /**
     * Find and return an account from a last name
     *
     * @param lastName  last name of account to find
     * @return          account object containing account found
     */
    public Account getAccountByLastName(String lastName) {
        Account account = accountRepository
                .findByLastName(lastName)
                .orElseThrow(AccountNotFoundException::new);

        return account;
    }

    public Account getAccountByID(int id) {
        Account account = accountRepository
                .findById(id)
                .orElseThrow(AccountNotFoundException::new);

        return account;
    }

    /**
     * Deposit an amount into an account
     *
     * @param lastName  last name of account to deposit into
     * @param amount    amount to deposit
     * @return          updated DTO of account
     */
    public AccountDTO deposit(String lastName, BigDecimal amount) {
        //Check if deposit is valid
        if (!isValidDeposit(lastName, amount)) {
            throw new DepositLimitException();
        }

        //Make deposit if it is valid
        Account accountToUpdate = accountRepository
                .findByLastName(lastName)
                .orElseThrow(AccountNotFoundException::new);
        BigDecimal oldBalance = accountToUpdate.getBalance();
        accountToUpdate.setBalance(oldBalance.add(amount));
        Account save = accountRepository.save(accountToUpdate);
        Transaction transaction = new Transaction(accountToUpdate, "deposit", amount);
        transactionRepository.save(transaction);
        return mapAccountToDTO(save);
    }

    /**
     * Determines if a deposit is valid. A deposit is valid iff the account's daily deposit total added to the new
     * deposit amount is less than the daily deposit limit which is $5,000
     *
     * @param lastName          last name of account being deposited to
     * @param depositAmount     amount being deposited
     * @return                  a boolean containing if the deposit is valid or not
     */
    private boolean isValidDeposit(String lastName, BigDecimal depositAmount) {
        final BigDecimal DEPOSIT_LIMIT = BigDecimal.valueOf(5000);

        //Get the current date and time and reset to very end of previous day
        LocalDate currentDay = LocalDate.now();

        //Get current daily deposit total
        ArrayList<Transaction> transactions = transactionRepository.findAllByAccount(getAccountByLastName(lastName));
        BigDecimal dailyDepositTotal = BigDecimal.ZERO;
        for (Transaction transaction : transactions) {
            if (transaction.getDate().isEqual(currentDay) && transaction.getType().equals("deposit")) {
                dailyDepositTotal = dailyDepositTotal.add(transaction.getAmount());
            }
        }

        //Compare dailyDepositTotal plus deposit amount to deposit limit and return value
        dailyDepositTotal = dailyDepositTotal.add(depositAmount);
        return (dailyDepositTotal.compareTo(DEPOSIT_LIMIT) < 1);

    }

    /**
     * Withdraw an amount from an account
     *
     * @param lastName  last name of account to withdraw from
     * @param amount    amount to withdraw
     * @return          DTO of updated account
     */
    public AccountDTO withdraw(String lastName, BigDecimal amount) {
        Account accountToUpdate = getAccountByLastName(lastName);
        BigDecimal oldBalance = accountToUpdate.getBalance();
        if (oldBalance.subtract(amount).compareTo(BigDecimal.ZERO) == -1) {
            throw new InsufficientFundsException();
        }
        accountToUpdate.setBalance(oldBalance.subtract(amount));
        Account save = accountRepository.save(accountToUpdate);
        Transaction transaction = new Transaction(accountToUpdate, "deposit", amount);
        transactionRepository.save(transaction);
        return mapAccountToDTO(save);
    }

    private void validateLastNameUnique(String lastName) {
        accountRepository
                .findByLastName(lastName)
                .ifPresent(t -> {throw new AccountLastNameExistsException();});
    }

    private AccountDTO mapAccountToDTO(Account account) {
        return new AccountDTO()
                .firstName(account.getFirstName())
                .lastName(account.getLastName())
                .balance(account.getBalance())
                .notificationPreference(account.getNotificationPreference());
    }

    /**
     * Transfers money from one account to another
     *
     * @param from      last name of account to transfer from
     * @param to        last name of account to transfer to
     * @param amount    amount to transfer
     */
    public void transfer(String from, String to, BigDecimal amount) {
        withdraw(from, amount);
        deposit(to, amount);
    }

    /**
     * Gets the most recent ten transactions from an account
     *
     * @param lastName  last name of account to find transactions of
     * @return          list containing transactionDTOs for ten most recent transactions
     */
    public List<TransactionDTO> getLatestTenTransaction(String lastName) {
        Account account = getAccountByLastName(lastName);
        ArrayList<Transaction> allTransactions = transactionRepository.findAllByAccount(account);
        if (allTransactions.size() > 10) {
            return mapTransactionListToDTOList(allTransactions.subList(allTransactions.size() - 10, allTransactions.size()));
        }
        else {
            return mapTransactionListToDTOList(allTransactions);
        }
    }

    /**
     * Maps a list of transactions to a list of transactionDTOs
     *
     * @param allTransactions   list of transactions
     * @return                  list of transactionDTOs
     */
    public List<TransactionDTO> mapTransactionListToDTOList(List<Transaction> allTransactions) {
        List<TransactionDTO> transactionDTOList = new ArrayList<>();
        for (Transaction transaction : allTransactions) {
            transactionDTOList.add(mapTransactionToDTO(transaction));
        }
        return transactionDTOList;
    }

    /**
     * Maps a transaction to a transactionDTO
     *
     * @param transaction   transaction to map
     * @return              mapped transaction in DTO format
     */
    private TransactionDTO mapTransactionToDTO(Transaction transaction) {
        TransactionDTO transactionDTO = new TransactionDTO(transaction.getType(), transaction.getAmount());
        return transactionDTO;
    }

}
