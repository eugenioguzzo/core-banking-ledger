package com.eugeniokg.corebankingledger.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Access token obtained from POST /auth/login or POST /auth/refresh."
)
public class OpenApiConfig {

    @Bean
    public OpenAPI coreBankingLedgerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Core Banking Ledger API")
                        .description("""
                                Double-entry bookkeeping ledger with idempotent transfers, JWT authentication \
                                and role-based authorization (CUSTOMER, OPERATOR, ADMIN).""")
                        .version("1.0")
                        .contact(new Contact().name("core-banking-ledger")));
    }
}
