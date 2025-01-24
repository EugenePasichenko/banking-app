package com.pasichenko.banking;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class BankingApplication {

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(BankingApplication.class, args);
	}


}
