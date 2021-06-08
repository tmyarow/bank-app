package com.ippon.bankapp.service;

import com.ippon.bankapp.domain.Account;
import com.ippon.bankapp.domain.Transaction;
import com.ippon.bankapp.repository.AccountRepository;
import com.ippon.bankapp.repository.TransactionRepository;
import com.ippon.bankapp.service.dto.AccountDTO;
import com.ippon.bankapp.service.dto.TransactionDTO;
import com.ippon.bankapp.service.exception.DepositLimitException;
import com.ippon.bankapp.service.exception.InsufficientFundsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private NotificationFactory notificationFactory;

    @Mock
    private EmailService emailService;

    @InjectMocks
    public AccountService subject;


    @Test
    public void createsAccount() {
        //Given
        AccountDTO accountDto = new AccountDTO()
                .firstName("Ben")
                .lastName("Scott");

        given(notificationFactory.getDefaultNotification())
            .willReturn(emailService);

        given(emailService.getName()).willReturn("email");

        given(notificationFactory.getPreferredService("email"))
                .willReturn(Optional.of(emailService));

        Account account = new Account(accountDto.getFirstName(), accountDto.getLastName());
        account.setNotificationPreference("email");

        given(accountRepository.save(account)).willReturn(account);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);

        //act
        AccountDTO accountResult = subject.createAccount(accountDto);

        //assert
        assertThat(accountResult.getBalance(), is(BigDecimal.ZERO));
        assertThat(accountResult.getNotificationPreference(), is("email"));
        assertThat(accountResult.getFirstName(), is("Ben"));
        assertThat(accountResult.getLastName(), is("Scott"));

        verify(emailService, times(1))
                .sendMessage(message.capture(), message.capture(), message.capture(), message.capture());
        assertThat(message.getAllValues().get(0), is("bank"));
        assertThat(message.getAllValues().get(1), is(accountDto.getLastName()));
        assertThat(message.getAllValues().get(2), is("Account Created"));
        assertThat(message.getAllValues().get(3), is("Welcome aboard!"));
    }

    @Test
    public void testDeposit() {
        Account account = new Account();
        account.setFirstName("Ben");
        account.setLastName("Scott");
        account.setBalance(BigDecimal.ZERO);
        given(accountRepository.findByLastName("Scott")).willReturn(Optional.of(account));
        given(accountRepository.save(account)).willReturn(account);

        AccountDTO result = subject.deposit("Scott", BigDecimal.valueOf(100));
        assertThat(result.getBalance(), is(BigDecimal.valueOf(100)));
    }

    @Test
    public void testDepositLimit() {
        Account account = new Account();
        account.setFirstName("Ben");
        account.setLastName("Scott");
        account.setBalance(BigDecimal.ZERO);
        given(accountRepository.findByLastName("Scott")).willReturn(Optional.of(account));
        given(accountRepository.save(account)).willReturn(account);

        AccountDTO result = subject.deposit("Scott", BigDecimal.valueOf(100));
        assertThat(result.getBalance(), is(BigDecimal.valueOf(100)));

        assertThrows(DepositLimitException.class,() -> subject.deposit("Scott", BigDecimal.valueOf(5100)));
    }

    @Test
    public void testInvalidWithdraw() {
        Account account = new Account();
        account.setFirstName("Ben");
        account.setLastName("Scott");
        account.setBalance(BigDecimal.ZERO);
        given(accountRepository.findByLastName("Scott")).willReturn(Optional.of(account));
        given(accountRepository.save(account)).willReturn(account);

        AccountDTO result = subject.deposit("Scott", BigDecimal.valueOf(100));
        assertThat(result.getBalance(), is(BigDecimal.valueOf(100)));

        assertThrows(InsufficientFundsException.class,() -> subject.withdraw("Scott", BigDecimal.valueOf(101)));
    }

    @Test
    public void testValidWithdraw() {
        Account account = new Account();
        account.setFirstName("Ben");
        account.setLastName("Scott");
        account.setBalance(BigDecimal.valueOf(100));
        given(accountRepository.findByLastName("Scott")).willReturn(Optional.of(account));
        given(accountRepository.save(account)).willReturn(account);

        AccountDTO result = subject.withdraw("Scott", BigDecimal.valueOf(50));
        assertThat(result.getBalance(), is(BigDecimal.valueOf(50)));
    }

    @Test
    public void testTransfer() {
        //Set up accounts
        Account account1 = new Account("first1", "last1");
        account1.setBalance(BigDecimal.valueOf(100));
        given(accountRepository.findByLastName("last1")).willReturn(Optional.of(account1));
        given(accountRepository.save(account1)).willReturn(account1);
        Account account2 = new Account("first2", "last2");
        account2.setId(3);
        given(accountRepository.findByLastName("last2")).willReturn(Optional.of(account2));
        given(accountRepository.save(account2)).willReturn(account2);

        //Do transfer
        subject.transfer("last1", "last2", BigDecimal.valueOf(50));

        //Assert that balances have been updated correctly
        assertThat(account1.getBalance(), is(BigDecimal.valueOf(50)));
        assertThat(account2.getBalance(), is(BigDecimal.valueOf(50)));
    }

    @Test
    public void testLatestTransactionsNotTen() {
        Account account = new Account("Tyler", "Yarow");
        given(accountRepository.save(account)).willReturn(account);
        given(accountRepository.findByLastName("Yarow")).willReturn(Optional.of(account));
        subject.deposit("Yarow", BigDecimal.valueOf(100));

        Transaction transaction = new Transaction(account, "deposit", BigDecimal.valueOf(100));
        ArrayList<Transaction> transactionList = new ArrayList<>();
        transactionList.add(transaction);
        given(transactionRepository.findAllByAccount(account)).willReturn(transactionList);

        assertThat(subject.getLatestTenTransaction("Yarow").get(0).getType(), is("deposit"));
    }

    @Test
    public void testLatestTransactionsOverTen() {
        //Setup account
        Account account = new Account("Tyler", "Yarow");
        given(accountRepository.save(account)).willReturn(account);
        given(accountRepository.findByLastName("Yarow")).willReturn(Optional.of(account));

        //Make 11 deposits
        subject.deposit("Yarow", BigDecimal.valueOf(1));
        subject.deposit("Yarow", BigDecimal.valueOf(2));
        subject.deposit("Yarow", BigDecimal.valueOf(3));
        subject.deposit("Yarow", BigDecimal.valueOf(4));
        subject.deposit("Yarow", BigDecimal.valueOf(5));
        subject.deposit("Yarow", BigDecimal.valueOf(6));
        subject.deposit("Yarow", BigDecimal.valueOf(7));
        subject.deposit("Yarow", BigDecimal.valueOf(8));
        subject.deposit("Yarow", BigDecimal.valueOf(9));
        subject.deposit("Yarow", BigDecimal.valueOf(10));
        subject.deposit("Yarow", BigDecimal.valueOf(11));

        //Create list to hold correct transaction list to test against
        ArrayList<Transaction> transactionList = new ArrayList<>();
        Transaction transaction1 = new Transaction(account, "deposit", BigDecimal.valueOf(1));
        transactionList.add(transaction1);
        Transaction transaction2 = new Transaction(account, "deposit", BigDecimal.valueOf(2));
        transactionList.add(transaction2);
        Transaction transaction3 = new Transaction(account, "deposit", BigDecimal.valueOf(3));
        transactionList.add(transaction3);
        Transaction transaction4 = new Transaction(account, "deposit", BigDecimal.valueOf(4));
        transactionList.add(transaction4);
        Transaction transaction5 = new Transaction(account, "deposit", BigDecimal.valueOf(5));
        transactionList.add(transaction5);
        Transaction transaction6 = new Transaction(account, "deposit", BigDecimal.valueOf(6));
        transactionList.add(transaction6);
        Transaction transaction7 = new Transaction(account, "deposit", BigDecimal.valueOf(7));
        transactionList.add(transaction7);
        Transaction transaction8 = new Transaction(account, "deposit", BigDecimal.valueOf(8));
        transactionList.add(transaction8);
        Transaction transaction9 = new Transaction(account, "deposit", BigDecimal.valueOf(9));
        transactionList.add(transaction9);
        Transaction transaction10 = new Transaction(account, "deposit", BigDecimal.valueOf(10));
        transactionList.add(transaction10);
        Transaction transaction11 = new Transaction(account, "deposit", BigDecimal.valueOf(11));
        transactionList.add(transaction11);
        given(transactionRepository.findAllByAccount(account)).willReturn(transactionList);

        //Assert that list contains correct transaction amount and type for each transaction in list
        for (int i = 0; i < 10; i++) {
            assertThat(subject.getLatestTenTransaction("Yarow")
                    .get(i).getAmount(), is(BigDecimal.valueOf(i + 2)));
            assertThat(subject.getLatestTenTransaction("Yarow").get(i).getType(), is("deposit"));
        }
    }

}
