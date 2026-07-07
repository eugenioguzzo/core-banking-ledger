package com.eugeniokg.corebankingledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CoreBankingLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreBankingLedgerApplication.class, args);
    }

}
