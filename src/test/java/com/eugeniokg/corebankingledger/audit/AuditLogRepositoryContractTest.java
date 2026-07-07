package com.eugeniokg.corebankingledger.audit;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the "immutable audit trail" contract at the API level: no method that could update
 * or delete an {@link AuditLog} is ever declared on the repository.
 */
class AuditLogRepositoryContractTest {

    @Test
    void exposesNoUpdateOrDeleteMethods() {
        String[] methodNames = Arrays.stream(AuditLogRepository.class.getMethods())
                .map(Method::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .toArray(String[]::new);

        assertThat(methodNames).noneMatch(name -> name.contains("delete"));
        assertThat(methodNames).noneMatch(name -> name.startsWith("update"));
    }
}
