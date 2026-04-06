package com.diegoanyosa.payments.command.handler;

import com.diegoanyosa.payments.command.model.UserAccount;
import com.diegoanyosa.payments.dto.request.CreateAccountRequest;
import com.diegoanyosa.payments.dto.response.AccountResponse;
import com.diegoanyosa.payments.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCommandHandler {

    private final UserAccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest req) {
        if (accountRepository.existsById(req.getUserId())) {
            throw new IllegalStateException("Account already exists for user: " + req.getUserId());
        }
        UserAccount account = UserAccount.builder()
            .userId(req.getUserId())
            .balance(req.getInitialBalance())
            .currency(req.getCurrency())
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        accountRepository.save(account);
        log.info("Account created for user: {}", req.getUserId());
        return toResponse(account);
    }

    @Transactional
    public AccountResponse deactivateAccount(String userId) {
        UserAccount account = accountRepository.findById(userId)
            .orElseThrow(() -> new com.diegoanyosa.payments.exception.AccountNotFoundException(userId));
        account.setActive(false);
        accountRepository.save(account);
        log.info("Account deactivated for user: {}", userId);
        return toResponse(account);
    }

    private AccountResponse toResponse(UserAccount a) {
        return AccountResponse.builder()
            .userId(a.getUserId()).balance(a.getBalance())
            .currency(a.getCurrency()).active(a.isActive()).build();
    }
}
