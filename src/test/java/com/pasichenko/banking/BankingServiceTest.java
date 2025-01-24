package com.pasichenko.banking;

import com.pasichenko.banking.entity.Account;
import com.pasichenko.banking.entity.Transaction;
import com.pasichenko.banking.repository.AccountRepository;
import com.pasichenko.banking.service.BankingService;
import com.pasichenko.banking.service.TransactionService;
import com.pasichenko.banking.service.impl.BankingServiceImpl;
import com.pasichenko.banking.service.impl.TransactionServiceImpl;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.annotation.EnableRetry;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@EnableRetry
@SpringBootTest
class BankingServiceTest {

    @Autowired
    private BankingService bankingService;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private TransactionService transactionService;

    private Account account;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Initialize Account for testing
        account = new Account();
        account.setId(1L);
        account.setAccountNumber("12345");
        account.setAccountType("SAVINGS");
        account.setBalance(BigDecimal.valueOf(1000));
        account.setVersion(1);
    }

    @Test
    void testDeposit() {
        // Arrange
        BigDecimal depositAmount = BigDecimal.valueOf(500);
        when(accountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));

        // Act
        bankingService.deposit(account.getAccountNumber(), depositAmount);

        // Assert
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(transactionService, times(1)).recordTransaction(eq(account), eq(Transaction.TransactionType.DEPOSIT), eq(depositAmount), isNull());
        assert(account.getBalance().compareTo(BigDecimal.valueOf(1500)) == 0); // Check balance is updated
    }

    @Test
    void testWithdraw() {
        // Arrange
        BigDecimal withdrawAmount = BigDecimal.valueOf(300);
        when(accountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));

        // Act
        bankingService.withdraw(account.getAccountNumber(), withdrawAmount);

        // Assert
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(transactionService, times(1)).recordTransaction(eq(account), eq(Transaction.TransactionType.WITHDRAW), eq(withdrawAmount), isNull());
        assert(account.getBalance().compareTo(BigDecimal.valueOf(700)) == 0); // Check balance is updated
    }

    @Test
    void testTransfer() {
        // Arrange
        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setAccountNumber("67890");
        toAccount.setAccountType("CHECKING");
        toAccount.setBalance(BigDecimal.valueOf(500));

        BigDecimal transferAmount = BigDecimal.valueOf(200);

        when(accountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));
        when(accountRepository.findByAccountNumber(toAccount.getAccountNumber())).thenReturn(Optional.of(toAccount));

        // Act
        bankingService.transfer(account.getAccountNumber(), toAccount.getAccountNumber(), transferAmount);

        // Assert
        verify(accountRepository, times(2)).save(any(Account.class));  // Ensures both accounts are saved
        verify(transactionService, times(1)).recordTransaction(eq(account), eq(Transaction.TransactionType.TRANSFER), eq(transferAmount), isNull());
        verify(transactionService, times(1)).recordTransaction(eq(toAccount), eq(Transaction.TransactionType.TRANSFER), eq(transferAmount), any());
        assert(account.getBalance().compareTo(BigDecimal.valueOf(800)) == 0); // Check balance for from account
        assert(toAccount.getBalance().compareTo(BigDecimal.valueOf(700)) == 0); // Check balance for to account
    }

    @Test
    void testDepositAccountNotFound() {
        // Arrange
        when(accountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                bankingService.deposit(account.getAccountNumber(), BigDecimal.valueOf(500))
        );
        assert(exception.getMessage().contains("Account not found"));
    }

    @Test
    void testWithdrawInsufficientBalance() {
        // Arrange
        BigDecimal withdrawAmount = BigDecimal.valueOf(2000);  // Amount more than balance
        when(accountRepository.findByAccountNumber(account.getAccountNumber())).thenReturn(Optional.of(account));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                bankingService.withdraw(account.getAccountNumber(), withdrawAmount)
        );
        assert(exception.getMessage().contains("Insufficient balance"));
    }

    @Test
    void testTransferWithRetry() {
        when(accountRepository.findByAccountNumber(anyString()))
                .thenThrow(new OptimisticLockException("Retrying"));

        assertThrows(OptimisticLockException.class, () -> {
            bankingService.transfer("fromAccount", "toAccount", BigDecimal.valueOf(100));
        });

        // Verify retry behavior
        verify(accountRepository, times(3)).findByAccountNumber(anyString());
    }
}
