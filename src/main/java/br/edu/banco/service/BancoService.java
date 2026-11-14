package br.edu.banco.service;

import br.edu.banco.api.Requisicoes.*;
import br.edu.banco.model.*;
import br.edu.banco.repository.*;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BancoService {
    private final ClienteRepository clientes;
    private final ContaRepository contas;
    private final BoletoRepository boletos;
    private final MovimentacaoRepository movimentacoes;
    private final boolean falharBancoCentral;

    public BancoService(ClienteRepository clientes, ContaRepository contas,
                        BoletoRepository boletos, MovimentacaoRepository movimentacoes,
                        @Value("${banco.central.falhar:false}") boolean falharBancoCentral) {
        this.clientes = clientes;
        this.contas = contas;
        this.boletos = boletos;
        this.movimentacoes = movimentacoes;
        this.falharBancoCentral = falharBancoCentral;
    }

    public Cliente criarCliente(NovoCliente dados) {
        if (clientes.existsByCpf(dados.cpf())) {
            throw new IllegalArgumentException("CPF ja cadastrado");
        }
        Cliente cliente = new Cliente();
        cliente.setNome(dados.nome().trim());
        cliente.setCpf(dados.cpf());
        return clientes.save(cliente);
    }

    public List<Cliente> listarClientes() {
        return clientes.findAll();
    }

    public Conta criarConta(NovaConta dados) {
        if (!clientes.existsById(dados.clienteId())) {
            throw naoEncontrado("Cliente");
        }
        String numero = dados.numero().trim();
        if (contas.existsByNumero(numero)) {
            throw new IllegalArgumentException("Numero de conta ja cadastrado");
        }
        Conta conta = new Conta();
        conta.setClienteId(dados.clienteId());
        conta.setNumero(numero);
        conta.setSaldoCentavos(0);
        return contas.save(conta);
    }

    public List<Conta> listarContas() {
        return contas.findAll();
    }

    public Conta consultarConta(Long id) {
        return contas.findById(id).orElseThrow(() -> naoEncontrado("Conta"));
    }

    public Movimentacao depositar(Long contaId, long valorCentavos) {
        validarValor(valorCentavos);
        Conta conta = consultarConta(contaId);
        creditar(conta, valorCentavos);
        // Deposito existe apenas para carregar saldo na demonstracao.
        return registrar(null, contaId, null, "DEPOSITO", "SALDO", valorCentavos,
                "Deposito de demonstracao", true);
    }

    public Movimentacao transferir(NovaTransferencia dados) {
        validarValor(dados.valorCentavos());
        String tipo = validarTipo(dados.tipo(), List.of("PIX", "TED", "DOC"));
        if (dados.origemId().equals(dados.destinoId())) {
            throw new IllegalArgumentException("Origem e destino devem ser diferentes");
        }
        // 1. Buscar as duas contas no SQLite.
        Conta origem = consultarConta(dados.origemId());
        Conta destino = consultarConta(dados.destinoId());

        // 2. Validar saldo. Recusa tambem fica no historico.
        boolean aprovada = origem.getSaldoCentavos() >= dados.valorCentavos();
        if (aprovada) {
            // 3. Debitar e creditar dentro da MESMA transacao.
            creditar(destino, dados.valorCentavos());
            origem.setSaldoCentavos(origem.getSaldoCentavos() - dados.valorCentavos());
        }
        // 4. Registrar historico e protocolo simulado do Banco Central.
        // 5. O Spring confirma tudo ao terminar este metodo sem erro.
        return registrar(origem.getId(), destino.getId(), null, "TRANSFERENCIA", tipo,
                dados.valorCentavos(), "Transferencia " + tipo, aprovada);
    }

    public Boleto criarBoleto(NovoBoleto dados) {
        validarValor(dados.valorCentavos());
        String codigo = dados.codigo().trim();
        if (boletos.existsByCodigo(codigo)) {
            throw new IllegalArgumentException("Codigo de boleto ja cadastrado");
        }
        Boleto boleto = new Boleto();
        boleto.setCodigo(codigo);
        boleto.setValorCentavos(dados.valorCentavos());
        boleto.setVencimento(dados.vencimento().toString());
        boleto.setPago(false);
        return boletos.save(boleto);
    }

    public List<Boleto> listarBoletos() {
        return boletos.findAll();
    }

    public Movimentacao pagarBoleto(Long boletoId, Long contaId) {
        Boleto boleto = boletos.findById(boletoId).orElseThrow(() -> naoEncontrado("Boleto"));
        if (boleto.isPago()) {
            throw new IllegalArgumentException("Boleto ja foi pago");
        }
        Conta conta = consultarConta(contaId);
        boolean aprovada = conta.getSaldoCentavos() >= boleto.getValorCentavos();
        if (aprovada) {
            conta.setSaldoCentavos(conta.getSaldoCentavos() - boleto.getValorCentavos());
            boleto.setPago(true);
        }
        return registrar(contaId, null, boletoId, "PAGAMENTO", "BOLETO",
                boleto.getValorCentavos(), boleto.getCodigo(), aprovada);
    }

    public Movimentacao pagar(NovoPagamento dados) {
        validarValor(dados.valorCentavos());
        String tipo = validarTipo(dados.tipo(), List.of("PIX", "SALDO"));
        Conta conta = consultarConta(dados.contaId());
        boolean aprovada = conta.getSaldoCentavos() >= dados.valorCentavos();
        if (aprovada) {
            conta.setSaldoCentavos(conta.getSaldoCentavos() - dados.valorCentavos());
        }
        return registrar(conta.getId(), null, null, "PAGAMENTO", tipo,
                dados.valorCentavos(), dados.referencia().trim(), aprovada);
    }

    public List<Movimentacao> consultarHistorico(Long contaId, int pagina) {
        consultarConta(contaId);
        if (pagina < 0) {
            throw new IllegalArgumentException("Pagina nao pode ser negativa");
        }
        return movimentacoes.findByOrigemIdOrDestinoIdOrderByIdDesc(
                contaId, contaId, PageRequest.of(pagina, 50));
    }

    private Movimentacao registrar(Long origemId, Long destinoId, Long boletoId,
                                   String natureza, String tipo, long valor,
                                   String referencia, boolean aprovada) {
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setOrigemId(origemId);
        movimentacao.setDestinoId(destinoId);
        movimentacao.setBoletoId(boletoId);
        movimentacao.setNatureza(natureza);
        movimentacao.setTipo(tipo);
        movimentacao.setValorCentavos(valor);
        movimentacao.setReferencia(referencia);
        movimentacao.setSituacao(aprovada ? "APROVADA" : "RECUSADA");
        movimentacao.setDataHora(Instant.now().toString());
        movimentacao.setProtocoloBancoCentral(
                natureza.equals("DEPOSITO") ? "NAO_APLICAVEL" : comunicarBancoCentral());
        return movimentacoes.save(movimentacao);
    }

    private String comunicarBancoCentral() {
        // Nao ha chamada de rede nem acesso ao Banco Central real.
        if (falharBancoCentral) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Banco Central simulado indisponivel; operacao desfeita");
        }
        return "SIM-" + UUID.randomUUID();
    }

    private void creditar(Conta conta, long valor) {
        conta.setSaldoCentavos(Math.addExact(conta.getSaldoCentavos(), valor));
    }

    private void validarValor(long valor) {
        if (valor <= 0 || valor > 100000000000L) {
            throw new IllegalArgumentException("Use de 1 a 100000000000 centavos");
        }
    }

    private String validarTipo(String tipo, List<String> permitidos) {
        String normalizado = tipo.trim().toUpperCase(Locale.ROOT);
        if (!permitidos.contains(normalizado)) {
            throw new IllegalArgumentException("Tipos permitidos: " + permitidos);
        }
        return normalizado;
    }

    private ResponseStatusException naoEncontrado(String recurso) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, recurso + " inexistente");
    }
}
