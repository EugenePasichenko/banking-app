package com.pasichenko.banking.service.impl;

import com.pasichenko.banking.entity.Account;
import com.pasichenko.banking.entity.Transaction;
import com.pasichenko.banking.repository.TransactionRepository;
import com.pasichenko.banking.service.TransactionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction recordTransaction(Account account, Transaction.TransactionType type, BigDecimal amount, Transaction reference) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setReferenceTransaction(reference);
        return transactionRepository.save(transaction);
    }
}
