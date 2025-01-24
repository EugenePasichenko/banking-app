package com.pasichenko.banking;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

@TestConfiguration
@Testcontainers
public class TestcontainersConfiguration {

	// Define and start PostgreSQL container
	@Container
	public static PostgreSQLContainer<?> postgresContainer =
			new PostgreSQLContainer<>("postgres:13")
					.withDatabaseName("testdb")
					.withUsername("testuser")
					.withPassword("testpass");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
		registry.add("spring.datasource.username", postgresContainer::getUsername);
		registry.add("spring.datasource.password", postgresContainer::getPassword);
	}

	static {
		postgresContainer.start();
	}

	// Define a DataSource bean if needed for other purposes
	@Bean
	@Primary
	public DataSource dataSource() {
		return DataSourceBuilder.create()
				.url(postgresContainer.getJdbcUrl())
				.username(postgresContainer.getUsername())
				.password(postgresContainer.getPassword())
				.driverClassName("org.postgresql.Driver")
				.build();
	}
}
