package br.edu.banco.api;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

// Cada record representa apenas os campos recebidos no JSON.
public final class Requisicoes {
    private Requisicoes() {
    }

    public record NovoCliente(
            @NotBlank @Size(max = 100) String nome,
            @NotBlank @Pattern(regexp = "[0-9]{11}") String cpf) {
    }

    public record NovaConta(
            @NotNull @Positive Long clienteId,
            @NotBlank @Size(max = 20) String numero) {
    }

    public record Valor(
            @NotNull @Positive @Max(100000000000L) Long valorCentavos) {
    }

    public record NovaTransferencia(
            @NotNull @Positive Long origemId,
            @NotNull @Positive Long destinoId,
            @NotNull @Positive @Max(100000000000L) Long valorCentavos,
            @NotBlank String tipo) {
    }

    public record NovoBoleto(
            @NotBlank @Size(max = 80) String codigo,
            @NotNull @Positive @Max(100000000000L) Long valorCentavos,
            @NotNull LocalDate vencimento) {
    }

    public record PagarBoleto(@NotNull @Positive Long contaId) {
    }

    public record NovoPagamento(
            @NotNull @Positive Long contaId,
            @NotNull @Positive @Max(100000000000L) Long valorCentavos,
            @NotBlank String tipo,
            @NotBlank @Size(max = 140) String referencia) {
    }
}
