package com.eugeniokg.corebankingledger.transaction;

import com.eugeniokg.corebankingledger.common.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transactions")
@Tag(name = "Transactions", description = "Double-entry money transfers between accounts")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @Operation(
            summary = "Transfer funds between two accounts",
            description = "Creates a Transaction plus two matching LedgerEntry records (a DEBIT on the "
                    + "source account and a CREDIT on the destination account). A CUSTOMER may only "
                    + "transfer from their own accounts, but may send to any destination account. "
                    + "Repeating the same Idempotency-Key returns the original result instead of "
                    + "executing the transfer again.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            { "sourceAccountId": "3fa85f64-5717-4562-b3fc-2c963f66afa6", \
                            "destinationAccountId": "9b2f3c1a-8e4d-4f3a-9c1a-8e4d4f3a9c1a", \
                            "amount": 100.00, "description": "Rent payment" }"""))))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer completed (or the original result, "
                    + "if this Idempotency-Key was already used)",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Missing Idempotency-Key header, or an invalid amount/account id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired access token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "The source account does not belong to the requesting CUSTOMER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Source or destination account not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Insufficient balance on the source account, "
                    + "or a concurrent update could not be resolved after retrying",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @Parameter(description = "Client-generated key that makes this request safe to retry",
                    required = true, example = "5f8d3c2a-1b4e-4f6a-9c3d-2e1f4a5b6c7d")
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        TransferResponse response = transactionService.transfer(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
