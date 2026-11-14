package br.edu.banco;

import br.edu.banco.api.Requisicoes.*;
import br.edu.banco.service.BancoService;
import br.edu.banco.repository.*;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@SpringBootTest(properties = {
        "banco.central.falhar=true",
        "spring.datasource.url=jdbc:sqlite:file:rollback-simples?mode=memory&cache=shared&foreign_keys=on&transaction_mode=IMMEDIATE"
})
class RollbackTest {
    @Autowired BancoService banco;
    @Autowired MovimentacaoRepository movimentacoes;
    @Autowired BoletoRepository boletos;

    @Test
    void falhaSimuladaDesfazTransferenciaEPagamento() {
        var cliente = banco.criarCliente(new NovoCliente("Ana", "11111111111"));
        var origem = banco.criarConta(new NovaConta(cliente.getId(), "1001"));
        var destino = banco.criarConta(new NovaConta(cliente.getId(), "1002"));
        banco.depositar(origem.getId(), 10000);
        assertThatThrownBy(() -> banco.transferir(new NovaTransferencia(
                origem.getId(), destino.getId(), 3000L, "PIX")))
                .isInstanceOf(ResponseStatusException.class);
        var boleto = banco.criarBoleto(new NovoBoleto("BOL-1", 2000L, LocalDate.of(2026, 11, 30)));
        assertThatThrownBy(() -> banco.pagarBoleto(boleto.getId(), origem.getId()))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(banco.consultarConta(origem.getId()).getSaldoCentavos()).isEqualTo(10000);
        assertThat(banco.consultarConta(destino.getId()).getSaldoCentavos()).isZero();
        assertThat(boletos.findById(boleto.getId()).orElseThrow().isPago()).isFalse();
        assertThat(movimentacoes.count()).isEqualTo(1); // Apenas o deposito.
    }
}
