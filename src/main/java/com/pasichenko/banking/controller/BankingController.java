package com.pasichenko.banking.controller;

import com.pasichenko.banking.service.impl.BankingServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.BankingApi;
import org.openapitools.model.DepositRequest;
import org.openapitools.model.TransferRequest;
import org.openapitools.model.WithdrawRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@Slf4j
public class BankingController implements BankingApi {

    private final BankingServiceImpl bankingService;

    public BankingController(BankingServiceImpl bankingService) {
        this.bankingService = bankingService;
    }

    @Override
    public ResponseEntity<Void> deposit(DepositRequest depositRequest) {
        bankingService.deposit(depositRequest.getAccountNumber(), BigDecimal.valueOf(depositRequest.getAmount()));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> withdraw(WithdrawRequest withdrawRequest) {
        bankingService.withdraw(withdrawRequest.getAccountNumber(), BigDecimal.valueOf(withdrawRequest.getAmount()));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> transfer(TransferRequest transferRequest) {
        bankingService.transfer(
                transferRequest.getFromAccountNumber(),
                transferRequest.getToAccountNumber(),
                BigDecimal.valueOf(transferRequest.getAmount())
        );
        return ResponseEntity.ok().build();
    }

    @Operation(hidden = true)  // hide from swagger , for jmeter usage only
    @GetMapping("/test-optimistic-locking")
    public ResponseEntity<String> testOptimisticLockingTransfer() {
        try {
            bankingService.transfer("10010001", "10010002", BigDecimal.valueOf(1));
            return ResponseEntity.ok("Transfer successful");
        }
        catch (ObjectOptimisticLockingFailureException op){
            log.error("Transfer failed, try later", op.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Transfer failed due to exception: " + op.getClass().getSimpleName());
        }
        catch (Exception e) {
            log.error("Transfer failed", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Transfer failed due to exception: " + e.getClass().getSimpleName());

        }
    }
}
