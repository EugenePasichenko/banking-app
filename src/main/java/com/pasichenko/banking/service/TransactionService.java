package com.pasichenko.banking.service;


import com.pasichenko.banking.entity.Account;
import com.pasichenko.banking.entity.Transaction;

import java.math.BigDecimal;

public interface TransactionService {

    public Transaction recordTransaction(Account account, Transaction.TransactionType type, BigDecimal amount, Transaction reference);
}
