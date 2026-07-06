# core-banking-ledger

Scaffolding di un servizio di ledger bancario, costruito con Spring Boot 3 e Java 21.
Il progetto espone (in futuro) API REST per la gestione di conti, transazioni e audit trail,
con autenticazione basata su JWT e persistenza su PostgreSQL.

> Stato attuale: solo scaffolding tecnico (build, configurazione, struttura a package).
> Nessuna logica di dominio è ancora implementata.

## Stack tecnico

- Java 21
- Spring Boot 3 (Web, Data JPA, Security, Validation)
- PostgreSQL + Flyway (migrazioni schema)
- springdoc-openapi (Swagger UI)
- JJWT (gestione token JWT)
- Testcontainers (test di integrazione con PostgreSQL reale)
- Lombok
- Maven

## Struttura dei package

```
com.eugeniokg.corebankingledger
├── config       # configurazione applicativa (beans, OpenAPI, ecc.)
├── security     # autenticazione, autorizzazione, JWT
├── account      # dominio conti
├── transaction  # dominio transazioni/movimenti di ledger
├── audit        # audit trail e compliance
└── common       # componenti condivisi (entità base, eccezioni, utility)
```

## Setup ambiente di sviluppo

### Prerequisiti

- JDK 21
- Docker (per PostgreSQL locale e per i test con Testcontainers)
- Maven (oppure il wrapper `mvnw` incluso nel progetto)

### Avvio del database locale

```bash
docker compose up -d
```

Avvia un container PostgreSQL su `localhost:5432` con:
- database: `core_banking_ledger`
- utente: `ledger`
- password: `ledger`

### Avvio dell'applicazione

```bash
mvn spring-boot:run
```

Di default viene attivato il profilo `dev`, che si connette al PostgreSQL locale avviato con
Docker Compose. L'API sarà disponibile su `http://localhost:8080` e la documentazione
OpenAPI su `http://localhost:8080/swagger-ui.html`.

### Test

I test di integrazione usano il profilo `test` e Testcontainers per avviare un'istanza
PostgreSQL effimera (richiede Docker attivo):

```bash
mvn test
```

## Profili applicativi

| Profilo | Descrizione                                              |
|---------|-----------------------------------------------------------|
| `dev`   | PostgreSQL locale (Docker Compose), per lo sviluppo       |
| `test`  | PostgreSQL avviato dinamicamente via Testcontainers        |

## Roadmap

- [ ] Modello di dominio per `account` e `transaction`
- [ ] Migrazioni Flyway per lo schema del ledger
- [ ] Autenticazione JWT e gestione utenti
- [ ] Audit trail delle operazioni
- [ ] Documentazione OpenAPI degli endpoint
