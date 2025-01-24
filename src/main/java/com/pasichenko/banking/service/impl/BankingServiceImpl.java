package com.pasichenko.banking.service.impl;

import com.pasichenko.banking.entity.Account;
import com.pasichenko.banking.entity.Transaction;
import com.pasichenko.banking.repository.AccountRepository;
import com.pasichenko.banking.service.BankingService;
import com.pasichenko.banking.service.TransactionService;
import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleObjectStateException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class BankingServiceImpl implements BankingService {

    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    public BankingServiceImpl(AccountRepository accountRepository, TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
    }

    @Retryable(
            value = {OptimisticLockException.class,StaleObjectStateException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    @Transactional(propagation = Propagation.REQUIRED)
    public void deposit(String accountNumber, BigDecimal amount) {

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        transactionService.recordTransaction(account, Transaction.TransactionType.DEPOSIT, amount, null);

    }

    @Retryable(
            value = {OptimisticLockException.class,StaleObjectStateException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    @Transactional(propagation = Propagation.REQUIRED)
    public void withdraw(String accountNumber, BigDecimal amount) {

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        transactionService.recordTransaction(account, Transaction.TransactionType.WITHDRAW, amount, null);

    }

    @Retryable(
            value = {OptimisticLockException.class,StaleObjectStateException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500)
    )
    @Transactional(propagation = Propagation.REQUIRED)
    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {

        if (RetrySynchronizationManager.getContext().getRetryCount() > 0) {
            log.warn("RETRIABLE EXECUTION");
            log.warn(RetrySynchronizationManager.getContext().toString());
        }
        Account fromAccount = accountRepository.findByAccountNumber(fromAccountNumber)
                    .orElseThrow(() -> new RuntimeException("Source account not found"));
        Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new RuntimeException("Destination account not found"));
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        Transaction debitTransaction = transactionService.recordTransaction(fromAccount, Transaction.TransactionType.TRANSFER, amount, null);
        transactionService.recordTransaction(toAccount, Transaction.TransactionType.TRANSFER, amount, debitTransaction);
    }


}
