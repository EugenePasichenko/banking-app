package com.pasichenko.banking;

import com.pasichenko.banking.service.BankingService;
import com.pasichenko.banking.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import com.pasichenko.banking.entity.Transaction;
import com.pasichenko.banking.service.impl.BankingServiceImpl;
import com.pasichenko.banking.service.impl.TransactionServiceImpl;
import com.pasichenko.banking.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestcontainersConfiguration.class})
@EnableRetry
public class BankingServiceIntegrationTest {

	@Autowired
	private BankingService bankingService;

	@MockBean
	private TransactionService transactionService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AccountRepository accountRepository;

	@BeforeEach
	public void setUp() {
		// Clean up before each test
		jdbcTemplate.execute("DELETE FROM transactions");
		jdbcTemplate.execute("DELETE FROM accounts");
		jdbcTemplate.execute("DELETE FROM users");
	}

	@Test
	public void testDeposit() {
		// Setup: create a user and an account
		jdbcTemplate.update("INSERT INTO users (full_name, email, phone) VALUES (?, ?, ?)",
				"John Doe", "john.doe@example.com", "1234567890");

		// Get the generated user ID
		Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", new Object[]{"john.doe@example.com"}, Long.class);

		// Insert account using the correct user ID
		jdbcTemplate.update("INSERT INTO accounts (user_id, account_number, account_type, balance, version) VALUES (?, ?, ?, ?, ?)",
				userId, "12345", "SAVINGS", 100.00, 0);

		// Perform deposit
		bankingService.deposit("12345", BigDecimal.valueOf(50));

		// Verify balance after deposit
		BigDecimal balance = jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE account_number = ?",
				new Object[]{"12345"}, BigDecimal.class);
		assert balance.compareTo(BigDecimal.valueOf(150.00)) == 0;

		// Verify transaction recorded
		verify(transactionService, times(1)).recordTransaction(any(), eq(Transaction.TransactionType.DEPOSIT), eq(BigDecimal.valueOf(50)), isNull());
	}


	@Test
	public void testWithdraw() {
		// Setup: create a user and an account
		jdbcTemplate.update("INSERT INTO users (full_name, email, phone) VALUES (?, ?, ?)",
				"Jane Doe", "jane.doe@example.com", "0987654321");

		// Verify that the user was inserted correctly
		Integer userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?",
				new Object[]{"jane.doe@example.com"}, Integer.class);
		assert userId != null;

		// Insert account for the user
		jdbcTemplate.update("INSERT INTO accounts (user_id, account_number, account_type, balance, version) VALUES (?, ?, ?, ?, ?)",
				userId, "54321", "CHECKING", 200.00, 0);

		// Perform withdrawal
		bankingService.withdraw("54321", BigDecimal.valueOf(50));

		// Verify balance after withdrawal
		BigDecimal balance = jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE account_number = ?",
				new Object[]{"54321"}, BigDecimal.class);
		assert balance.compareTo(BigDecimal.valueOf(150.00)) == 0;

		// Verify transaction recorded
		verify(transactionService, times(1)).recordTransaction(any(), eq(Transaction.TransactionType.WITHDRAW), eq(BigDecimal.valueOf(50)), isNull());
	}


	@Test
	public void testTransfer() {
		// Setup: create two users and retrieve their generated user IDs
		jdbcTemplate.update("INSERT INTO users (full_name, email, phone) VALUES (?, ?, ?)",
				"John Smith", "john.smith@example.com", "1231231234");
		jdbcTemplate.update("INSERT INTO users (full_name, email, phone) VALUES (?, ?, ?)",
				"Sarah Lee", "sarah.lee@example.com", "9879879876");

		// Get user IDs
		Long userId1 = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", new Object[]{"john.smith@example.com"}, Long.class);
		Long userId2 = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", new Object[]{"sarah.lee@example.com"}, Long.class);

		// Insert accounts using the correct user IDs
		jdbcTemplate.update("INSERT INTO accounts (user_id, account_number, account_type, balance, version) VALUES (?, ?, ?, ?, ?)",
				userId1, "11111", "CHECKING", 200.00, 0);
		jdbcTemplate.update("INSERT INTO accounts (user_id, account_number, account_type, balance, version) VALUES (?, ?, ?, ?, ?)",
				userId2, "22222", "SAVINGS", 50.00, 0);

		// Perform transfer
		bankingService.transfer("11111", "22222", BigDecimal.valueOf(100));

		// Verify balances after transfer
		BigDecimal balanceFrom = jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE account_number = ?",
				new Object[]{"11111"}, BigDecimal.class);
		BigDecimal balanceTo = jdbcTemplate.queryForObject("SELECT balance FROM accounts WHERE account_number = ?",
				new Object[]{"22222"}, BigDecimal.class);

		assert balanceFrom.compareTo(BigDecimal.valueOf(100.00)) == 0;
		assert balanceTo.compareTo(BigDecimal.valueOf(150.00)) == 0;

		// Verify transaction recorded for both accounts
		verify(transactionService, times(2)).recordTransaction(any(), eq(Transaction.TransactionType.TRANSFER), eq(BigDecimal.valueOf(100)), isNull());
	}

	@Test
	public void testOptimisticLockingDuringTransfer() throws InterruptedException {

		// Insert users
		jdbcTemplate.update("INSERT INTO users (full_name, email, phone) VALUES (?, ?, ?)", "John Smith", "john.smith@example.com", "1231231234");
		jdbcTemplate.update("INSERT INTO users (full_name, email, phone) VALUES (?, ?, ?)", "Sarah Lee", "sarah.lee@example.com", "9879879876");

		// Insert accounts with initial balance and version
		jdbcTemplate.update("INSERT INTO accounts (user_id, account_number, account_type, balance, version) VALUES (?, ?, ?, ?, ?)",
				1, "11111", "CHECKING", 200.00, 0);
		jdbcTemplate.update("INSERT INTO accounts (user_id, account_number, account_type, balance, version) VALUES (?, ?, ?, ?, ?)",
				2, "22222", "SAVINGS", 50.00, 0);

		// Simulate concurrent transfers
		CountDownLatch latch = new CountDownLatch(2);

		// Thread 1: Transfer money
		Thread transferThread1 = new Thread(() -> {
			bankingService.transfer("11111", "22222", BigDecimal.valueOf(50));
			latch.countDown();
		});

		// Thread 2: Transfer money
		Thread transferThread2 = new Thread(() -> {
			bankingService.transfer("11111", "22222", BigDecimal.valueOf(50));
			latch.countDown();
		});

		// Start both threads
		transferThread1.start();
		transferThread2.start();

		// Wait for both threads to finish
		latch.await();

		// Verify account balances
		BigDecimal balanceFrom = jdbcTemplate.queryForObject(
				"SELECT balance FROM accounts WHERE account_number = ?",
				new Object[]{"11111"},
				BigDecimal.class
		);
		BigDecimal balanceTo = jdbcTemplate.queryForObject(
				"SELECT balance FROM accounts WHERE account_number = ?",
				new Object[]{"22222"},
				BigDecimal.class
		);

		assert balanceFrom.compareTo(BigDecimal.valueOf(100.00)) == 0;
		assert balanceTo.compareTo(BigDecimal.valueOf(150.00)) == 0;

		// Verify that transaction was recorded
		verify(transactionService, atLeast(2)).recordTransaction(any(), eq(Transaction.TransactionType.TRANSFER), eq(BigDecimal.valueOf(50)), isNull());
	}

}

