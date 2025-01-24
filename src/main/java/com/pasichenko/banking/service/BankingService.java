package com.pasichenko.banking.service;

import java.math.BigDecimal;

public interface BankingService {

    void deposit(String accountNumber, BigDecimal amount);

    void withdraw(String accountNumber, BigDecimal amount);

    void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount);
}

