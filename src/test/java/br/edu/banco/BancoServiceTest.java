package br.edu.banco;

import br.edu.banco.api.Requisicoes.*;
import br.edu.banco.model.*;
import br.edu.banco.repository.*;
import br.edu.banco.service.BancoService;
import java.time.LocalDate;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS)
@SpringBootTest
@AutoConfigureMockMvc
class BancoServiceTest {
    @Autowired BancoService banco;
    @Autowired ClienteRepository clientes;
    @Autowired ContaRepository contas;
    @Autowired BoletoRepository boletos;
    @Autowired MovimentacaoRepository movimentacoes;
    @Autowired MockMvc http;

    @BeforeEach
    void limparBanco() {
        movimentacoes.deleteAll();
        boletos.deleteAll();
        contas.deleteAll();
        clientes.deleteAll();
    }

    Conta novaConta(String cpf, String numero) {
        Cliente cliente = banco.criarCliente(new NovoCliente("Aluno", cpf));
        return banco.criarConta(new NovaConta(cliente.getId(), numero));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PIX", "TED", "DOC"})
    void transferenciaDebitaCreditaERegistra(String tipo) {
        Conta origem = novaConta("11111111111", "1001");
        Conta destino = novaConta("22222222222", "1002");
        banco.depositar(origem.getId(), 10000);
        Movimentacao registro = banco.transferir(new NovaTransferencia(
                origem.getId(), destino.getId(), 3000L, tipo));
        assertThat(banco.consultarConta(origem.getId()).getSaldoCentavos()).isEqualTo(7000);
        assertThat(banco.consultarConta(destino.getId()).getSaldoCentavos()).isEqualTo(3000);
        assertThat(registro.getSituacao()).isEqualTo("APROVADA");
        assertThat(registro.getProtocoloBancoCentral()).startsWith("SIM-");
        assertThat(banco.consultarHistorico(destino.getId(), 0)).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PIX", "SALDO"})
    void pagamentoDebitaConta(String tipo) {
        Conta conta = novaConta("11111111111", "1001");
        banco.depositar(conta.getId(), 10000);
        banco.pagar(new NovoPagamento(conta.getId(), 2550L, tipo, "Loja"));
        assertThat(banco.consultarConta(conta.getId()).getSaldoCentavos()).isEqualTo(7450);
    }

    @Test
    void boletoNaoPodeSerPagoDuasVezes() {
        Conta conta = novaConta("11111111111", "1001");
        banco.depositar(conta.getId(), 10000);
        Boleto boleto = banco.criarBoleto(new NovoBoleto("BOL-1", 2000L, LocalDate.of(2026, 11, 30)));
        banco.pagarBoleto(boleto.getId(), conta.getId());
        assertThat(boletos.findById(boleto.getId()).orElseThrow().isPago()).isTrue();
        assertThatThrownBy(() -> banco.pagarBoleto(boleto.getId(), conta.getId()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(banco.consultarConta(conta.getId()).getSaldoCentavos()).isEqualTo(8000);
        assertThat(movimentacoes.count()).isEqualTo(2); // Deposito + pagamento.
    }

    @Test
    void recusaNaoAlteraSaldoMasGuardaHistorico() {
        Conta origem = novaConta("11111111111", "1001");
        Conta destino = novaConta("22222222222", "1002");
        Movimentacao registro = banco.transferir(new NovaTransferencia(
                origem.getId(), destino.getId(), 1L, "PIX"));
        assertThat(registro.getSituacao()).isEqualTo("RECUSADA");
        assertThat(registro.getProtocoloBancoCentral()).startsWith("SIM-");
        assertThat(banco.consultarConta(origem.getId()).getSaldoCentavos()).isZero();
        assertThat(banco.consultarConta(destino.getId()).getSaldoCentavos()).isZero();
        assertThat(movimentacoes.count()).isEqualTo(1);
    }

    @Test
    void boletoSemSaldoContinuaAberto() {
        Conta conta = novaConta("11111111111", "1001");
        Boleto boleto = banco.criarBoleto(new NovoBoleto("BOL-1", 2000L, LocalDate.of(2026, 11, 30)));
        assertThat(banco.pagarBoleto(boleto.getId(), conta.getId()).getSituacao()).isEqualTo("RECUSADA");
        assertThat(boletos.findById(boleto.getId()).orElseThrow().isPago()).isFalse();
    }

    @Test
    void saldoExatoPodeZerar() {
        Conta conta = novaConta("11111111111", "1001");
        banco.depositar(conta.getId(), 1);
        banco.pagar(new NovoPagamento(conta.getId(), 1L, "SALDO", "Teste"));
        assertThat(banco.consultarConta(conta.getId()).getSaldoCentavos()).isZero();
    }

    @Test
    void naoAceitaDuplicadosOuAutotransferencia() {
        Conta conta = novaConta("11111111111", "1001");
        assertThatThrownBy(() -> banco.criarCliente(new NovoCliente("Outro", "11111111111")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> banco.criarConta(new NovaConta(conta.getClienteId(), "1001")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> banco.transferir(new NovaTransferencia(
                conta.getId(), conta.getId(), 100L, "PIX"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(movimentacoes.count()).isZero();
    }

    @Test
    void httpValidaCamposEContaInexistente() throws Exception {
        http.perform(post("/api/contas/1/depositos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"valorCentavos\":0}")).andExpect(status().isBadRequest());
        http.perform(post("/api/contas/1/depositos").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
        http.perform(post("/api/contas/1/depositos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"valorCentavos\":1.5}")).andExpect(status().isBadRequest());
        http.perform(get("/api/contas/999999/saldo")).andExpect(status().isNotFound());
    }

    @Test
    void httpRetornaRecusaComRegistro() throws Exception {
        Conta conta = novaConta("11111111111", "1001");
        String json = "{\"contaId\":" + conta.getId()
                + ",\"valorCentavos\":1,\"tipo\":\"PIX\",\"referencia\":\"Loja\"}";
        http.perform(post("/api/pagamentos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.situacao").value("RECUSADA"));
    }

    @Test
    void duasTransferenciasNaoGastamOMesmoSaldo() throws Exception {
        Conta origem = novaConta("11111111111", "1001");
        Conta destino = novaConta("22222222222", "1002");
        banco.depositar(origem.getId(), 10000);
        CountDownLatch inicio = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<String> tarefa = () -> {
            inicio.await();
            return banco.transferir(new NovaTransferencia(
                    origem.getId(), destino.getId(), 8000L, "PIX")).getSituacao();
        };
        try {
            Future<String> primeira = pool.submit(tarefa);
            Future<String> segunda = pool.submit(tarefa);
            inicio.countDown();
            assertThat(java.util.List.of(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("APROVADA", "RECUSADA");
            assertThat(banco.consultarConta(origem.getId()).getSaldoCentavos()).isEqualTo(2000);
            assertThat(banco.consultarConta(destino.getId()).getSaldoCentavos()).isEqualTo(8000);
        } finally {
            pool.shutdownNow();
        }
    }
}
