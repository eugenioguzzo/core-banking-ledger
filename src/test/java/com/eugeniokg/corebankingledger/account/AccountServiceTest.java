package com.eugeniokg.corebankingledger.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void findByIdReturnsAccountWhenPresent() {
        UUID id = UUID.randomUUID();
        Account account = Account.builder().id(id).balance(BigDecimal.TEN).currency("EUR").status(AccountStatus.ACTIVE).build();
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        Account result = accountService.findById(id);

        assertThat(result).isEqualTo(account);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findById(id))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found with id: " + id);
    }
}
