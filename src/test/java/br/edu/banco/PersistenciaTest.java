package br.edu.banco;

import br.edu.banco.api.Requisicoes.*;
import br.edu.banco.service.BancoService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import static org.assertj.core.api.Assertions.*;

class PersistenciaTest {
    @TempDir Path pasta;

    @Test
    void saldoEHistoricoContinuamDepoisDeReabrirOBanco() {
        String url = "jdbc:sqlite:" + pasta.resolve("banco.db").toString().replace('\\', '/')
                + "?foreign_keys=on&transaction_mode=IMMEDIATE";
        Long contaId;
        try (var contexto = abrir(url)) {
            var banco = contexto.getBean(BancoService.class);
            var cliente = banco.criarCliente(new NovoCliente("Ana", "11111111111"));
            var conta = banco.criarConta(new NovaConta(cliente.getId(), "1001"));
            contaId = conta.getId();
            banco.depositar(contaId, 10000);
            banco.pagar(new NovoPagamento(contaId, 550L, "PIX", "Loja"));
        }
        // O primeiro contexto foi encerrado. Um novo contexto abre o MESMO arquivo.
        try (var contexto = abrir(url)) {
            var banco = contexto.getBean(BancoService.class);
            assertThat(banco.consultarConta(contaId).getSaldoCentavos()).isEqualTo(9450);
            var historico = banco.consultarHistorico(contaId, 0);
            assertThat(historico).hasSize(2);
            assertThat(historico.get(0).getProtocoloBancoCentral()).startsWith("SIM-");
        }
    }

    private ConfigurableApplicationContext abrir(String url) {
        return new SpringApplicationBuilder(BancoApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + url, "--spring.main.banner-mode=off");
    }
}
