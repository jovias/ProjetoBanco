# Roteiro curto de defesa

## 1. Mostrar a estrutura

Abra model, repository, service e api. Diga: “Separei as classes que armazenam dados, o acesso ao banco, as regras e os endpoints.”

## 2. Explicar uma conta

Abra Conta.java. @Entity informa que a classe é persistida; @Id e @GeneratedValue definem o identificador. saldoCentavos guarda dinheiro sem double. clienteId aponta para um cliente; schema.sql garante essa referência no banco. @Version ajuda a detectar alterações concorrentes.

## 3. Mostrar o repository

Abra ContaRepository. JpaRepository fornece findById, findAll e save. Não há implementação escrita à mão: o Spring Data cria a implementação e o Hibernate executa o SQL.

## 4. Acompanhar uma transferência

1. BancoController.transferir recebe origemId, destinoId, valorCentavos e tipo.
2. @Valid verifica campos obrigatórios e positivos.
3. BancoService.transferir procura as contas e rejeita origem igual a destino.
4. Se há saldo, credita o destino e debita a origem.
5. registrar cria a movimentação e comunicaBancoCentral gera um protocolo fictício.
6. Ao sair do método sem erro, @Transactional confirma saldo e movimentação na mesma transação.
7. O controller devolve 201 e os dados da movimentação. Se faltou saldo, devolve 422 com RECUSADA e preserva os saldos.

## 5. Demonstrar

Execute BancoApplication e scripts/demo.ps1 em um banco novo. Mostre o saldo final de Ana (4000 centavos), de Bia (3000) e o histórico de Ana (8 registros). Mostre que boleto pago não pode ser pago novamente.

## 6. Provar que foi salvo

Copie o ID de Ana. Pare e reinicie a aplicação na mesma pasta. Consulte GET /api/contas/ID. O saldo continua 4000. O arquivo é dados/banco.db. Opcional: abra-o em um visualizador SQLite e execute SELECT * FROM movimentacoes;.

## 7. Explicar atomicidade

Abra RollbackTest. Ativamos a falha do simulador após alterar os objetos. A exceção faz o Spring desfazer toda a operação: nenhum saldo ou boleto fica parcialmente alterado, e não aparece uma movimentação aprovada. Recusa por saldo não lança exceção porque queremos guardar a tentativa recusada.

## Perguntas que você deve saber responder

**Por que não há save depois de setSaldoCentavos?** A conta foi carregada dentro da transação e está gerenciada pelo JPA. O Hibernate detecta a alteração e grava no commit.

**Por que service separado do controller?** HTTP fica no controller; regras e transação ficam no service.

**Por que centavos?** Evita erros de arredondamento de double e segue o padrão simples da referência.

**O protocolo prova comunicação real?** Não, é só uma string gerada localmente para demonstrar o fluxo.

**Por que não há classes Pix/Ted/Doc?** São modalidades do mesmo fluxo; um campo tipo permite distingui-las sem duplicar regras.

**Como funciona boleto?** Primeiro cadastra; ao pagar com saldo suficiente, marca pago e debita na mesma transação. A segunda tentativa é rejeitada.

**O que foi simplificado?** Banco único, serviço único, relações por IDs, pagamentos externos fictícios e protocolo na movimentação. O arquivo DIAGRAMAS.md relaciona cada anexo ao código.
