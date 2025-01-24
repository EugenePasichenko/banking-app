
INSERT INTO users (id, full_name, email, phone)
VALUES
(1, 'John Doe', 'john.doe@example.com', '+1234567890'),
(2, 'Jane Smith', 'jane.smith@example.com', '+1234567891'),
(3, 'Alice Johnson', 'alice.j@example.com', '+1234567892'),
(4, 'Bob Brown', 'bob.brown@example.com', '+1234567893'),
(5, 'Charlie Williams', 'charlie.w@example.com', '+1234567894'),
(6, 'Eve Davis', 'eve.d@example.com', '+1234567895');

INSERT INTO accounts (id, user_id, account_number, account_type, balance, created_at)
VALUES
(1, 1, '10010001', 'Savings', 5000.00, NOW()),
(2, 2, '10010002', 'Checking', 3200.00, NOW()),
(3, 3, '10010003', 'Savings', 10000.00, NOW()),
(4, 4, '10010004', 'Checking', 1500.00, NOW()),
(5, 5, '10010005', 'Savings', 7800.00, NOW());
