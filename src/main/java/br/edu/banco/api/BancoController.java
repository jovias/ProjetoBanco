package br.edu.banco.api;

import br.edu.banco.api.Requisicoes.*;
import br.edu.banco.model.*;
import br.edu.banco.service.BancoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BancoController {
    private final BancoService banco;

    public BancoController(BancoService banco) {
        this.banco = banco;
    }

    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public Cliente criarCliente(@Valid @RequestBody NovoCliente dados) {
        return banco.criarCliente(dados);
    }

    @GetMapping("/clientes")
    public List<Cliente> listarClientes() {
        return banco.listarClientes();
    }

    @PostMapping("/contas")
    @ResponseStatus(HttpStatus.CREATED)
    public Conta criarConta(@Valid @RequestBody NovaConta dados) {
        return banco.criarConta(dados);
    }

    @GetMapping("/contas")
    public List<Conta> listarContas() {
        return banco.listarContas();
    }

    @GetMapping("/contas/{id}")
    public Conta consultarConta(@PathVariable Long id) {
        return banco.consultarConta(id);
    }

    @GetMapping("/contas/{id}/saldo")
    public Map<String, Long> consultarSaldo(@PathVariable Long id) {
        return Map.of("contaId", id, "saldoCentavos", banco.consultarConta(id).getSaldoCentavos());
    }

    @PostMapping("/contas/{id}/depositos")
    public ResponseEntity<Movimentacao> depositar(@PathVariable Long id,
                                                @Valid @RequestBody Valor dados) {
        return resposta(banco.depositar(id, dados.valorCentavos()));
    }

    @PostMapping("/transferencias")
    public ResponseEntity<Movimentacao> transferir(@Valid @RequestBody NovaTransferencia dados) {
        return resposta(banco.transferir(dados));
    }

    @PostMapping("/boletos")
    @ResponseStatus(HttpStatus.CREATED)
    public Boleto criarBoleto(@Valid @RequestBody NovoBoleto dados) {
        return banco.criarBoleto(dados);
    }

    @GetMapping("/boletos")
    public List<Boleto> listarBoletos() {
        return banco.listarBoletos();
    }

    @PostMapping("/boletos/{id}/pagamentos")
    public ResponseEntity<Movimentacao> pagarBoleto(@PathVariable Long id,
                                                  @Valid @RequestBody PagarBoleto dados) {
        return resposta(banco.pagarBoleto(id, dados.contaId()));
    }

    @PostMapping("/pagamentos")
    public ResponseEntity<Movimentacao> pagar(@Valid @RequestBody NovoPagamento dados) {
        return resposta(banco.pagar(dados));
    }

    @GetMapping("/contas/{id}/movimentacoes")
    public List<Movimentacao> historico(@PathVariable Long id,
                                        @RequestParam(defaultValue = "0") int pagina) {
        return banco.consultarHistorico(id, pagina);
    }

    private ResponseEntity<Movimentacao> resposta(Movimentacao movimentacao) {
        int status = movimentacao.getSituacao().equals("APROVADA") ? 201 : 422;
        return ResponseEntity.status(status).body(movimentacao);
    }
}
