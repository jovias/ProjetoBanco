# Banco Spring Boot + SQLite — versão simples

Projeto acadêmico organizado no padrão **model → repository → service → api**, inspirado na organização do [projeto de referência dos colegas](https://github.com/andrempg/banco-spring-sqlite). Implementação própria; o código deles não foi copiado. Eles usam JdbcTemplate; aqui foi mantido **JPA**, solicitado para esta entrega.

## Abrir no IntelliJ

A pasta deste projeto é a mesma da versão anterior já importada. Se o IntelliJ estiver aberto, os arquivos serão atualizados. Caso necessário, use File → Open e selecione o **pom.xml** como projeto Maven. Aguarde o Maven carregar as dependências.

1. Use JDK 21 ou superior (a versão executada nesta máquina está em VALIDACAO.md).
2. Abra **BancoApplication.java** em src/main/java/br/edu/banco.
3. Clique no triângulo verde ao lado de main.
4. A API fica em **http://127.0.0.1:8080/api**.

Pelo terminal, com Maven instalado:

~~~sh
mvn test
mvn spring-boot:run
~~~

Para gerar o executável: **mvn package**. Depois: **java -jar target/banco-1.0.0.jar**.

O banco fica em **dados/banco.db**. A pasta dados é criada ao executar BancoApplication. As tabelas são criadas por schema.sql, sem apagar registros existentes. Reinicie na mesma pasta para reutilizar o arquivo.

**Mudança em relação à versão anterior:** esta versão usa outro modelo e outro caminho de banco; não migra o antigo banco.db. Os exemplos HTTP antigos também mudaram. Siga os exemplos abaixo.

## O que estudar primeiro

~~~text
br.edu.banco
├── BancoApplication.java      inicia a aplicação
├── model                     classes gravadas no SQLite
│   ├── Cliente.java
│   ├── Conta.java
│   ├── Boleto.java
│   └── Movimentacao.java
├── repository                interfaces JpaRepository
├── service
│   └── BancoService.java     todas as regras em um lugar
└── api
    ├── BancoController.java  recebe as requisições
    ├── Requisicoes.java      campos de entrada e validações
    └── TratamentoErros.java  mensagens de erro
~~~

Os modelos usam atributos privados e getters/setters, sem Lombok. Os records em Requisicoes apenas agrupam os dados recebidos no JSON; não são tabelas. Leia primeiro Conta, ContaRepository, BancoService.transferir e BancoController.transferir.

## Funcionalidades

- Cadastrar/listar clientes e contas; consultar saldo.
- Fazer depósito de demonstração para carregar saldo.
- Transferir por Pix, TED ou DOC entre contas locais.
- Cadastrar boleto e pagá-lo uma única vez.
- Fazer pagamento Pix ou com saldo para uma referência externa simulada.
- Consultar histórico e protocolo simulado do Banco Central.
- Recusar saldo insuficiente sem movimentar dinheiro e guardar a tentativa.

**Todos os valores são em centavos.** 10000 significa R$ 100,00; 3000 significa R$ 30,00. O limite por operação é 100000000000 centavos. Não use números fracionados.

## Demonstração pronta

Com o servidor iniciado, execute uma vez em um banco novo, em outro PowerShell:

~~~powershell
powershell -ExecutionPolicy Bypass -File scripts/demo.ps1
~~~

O script cria Ana e Bia, deposita R$ 100,00, executa os seis tipos de operação e uma recusa. Resultado: **Ana R$ 40,00; Bia R$ 30,00**. Os outros R$ 30,00 representam três pagamentos externos simulados. São 8 movimentações na conta de Ana: depósito, 6 operações e recusa. Para repetir, altere CPFs/números de conta/código do boleto no script, pois esses campos são únicos.

## Endpoints

| Método | Caminho | Corpo JSON |
|---|---|---|
| POST | /api/clientes | {"nome":"Ana","cpf":"11111111111"} |
| GET | /api/clientes | — |
| POST | /api/contas | {"clienteId":1,"numero":"1001"} |
| GET | /api/contas | — |
| GET | /api/contas/1 | — |
| GET | /api/contas/1/saldo | — |
| POST | /api/contas/1/depositos | {"valorCentavos":10000} |
| POST | /api/transferencias | {"origemId":1,"destinoId":2,"valorCentavos":3000,"tipo":"PIX"} |
| POST | /api/boletos | {"codigo":"BOL-1","valorCentavos":2000,"vencimento":"2026-11-30"} |
| GET | /api/boletos | — |
| POST | /api/boletos/1/pagamentos | {"contaId":1} |
| POST | /api/pagamentos | {"contaId":1,"valorCentavos":500,"tipo":"PIX","referencia":"Loja"} |
| GET | /api/contas/1/movimentacoes?pagina=0 | — |

Troque os IDs pelos retornados ao cadastrar. Transferências aceitam PIX/TED/DOC; pagamentos diretos aceitam PIX/SALDO. Boleto é pago pelo endpoint próprio. Histórico retorna até 50 registros por página, do mais recente ao mais antigo.

201 = criado/aprovado; 400 = dados inválidos, repetidos ou boleto já pago; 404 = recurso ausente; 422 = saldo insuficiente, com registro RECUSADA; 409 = conflito na gravação; 503 = falha do simulador.

O protocolo **SIM-...** e a situação ficam em Movimentacao. Depósitos usam NAO_APLICAVEL porque são uma facilidade local de demonstração. CPF é validado apenas pelo formato de 11 dígitos; não verificamos dígitos verificadores.

## Persistência em seis passos

1. Controller recebe o JSON e valida os campos com @Valid.
2. BancoService busca objetos usando os repositories.
3. Valida contas, modalidade e saldo.
4. Altera o saldo dos objetos e cria Movimentacao.
5. JpaRepository salva o registro; Hibernate converte os objetos em SQL. Alterações em entidades buscadas dentro da transação são detectadas automaticamente, sem precisar chamar save para cada alteração de saldo.
6. @Transactional confirma tudo junto. Se ocorrer exceção, o Spring desfaz as gravações daquela operação.

Uma recusa por saldo é um resultado normal: registra RECUSADA, sem débito/crédito. Já uma falha no Banco Central simulado lança exceção e desfaz toda a operação. Para demonstrar essa falha, inicie com **mvn spring-boot:run -Dspring-boot.run.arguments=--banco.central.falhar=true**. Nesse modo depósitos continuam permitidos, mas pagamentos e transferências retornam 503.

## Simplificações dos nove diagramas

As responsabilidades foram mantidas como métodos de BancoService: consultarConta (Saldo/Conta), pagar (Pagamento Pix/Saldo), pagarBoleto (Boleto), transferir (Pix/TED/DOC), consultarHistorico e comunicarBancoCentral. Não há uma classe de serviço para cada modalidade. Veja [DIAGRAMAS.md](DIAGRAMAS.md).

- Aplicação única e SQLite substituem pods, gateway e PostgreSQL da implantação desenhada.
- JPA substitui o SQL manual do projeto de referência.
- IDs simples relacionam os modelos; as chaves estrangeiras ficam em schema.sql. Não há navegação @ManyToOne entre objetos, para manter leitura e JSON diretos.
- Protocolo e situação ficam na movimentação; não existe tabela separada de comunicações.
- Banco Central é só uma função que gera protocolo fictício; não há rede externa.
- Boleto não verifica código bancário nem juros ou vencimento; a data é apenas armazenada.
- TED/DOC/Pix são categorias didáticas de liquidação imediata local. DOC é preservado só como simulação do diagrama.
- Aprovação só volta após commit, corrigindo o retorno antecipado desenhado em alguns fluxos.

Uma conexão no pool serializa as transações desta aplicação. SQLite usa transação IMMEDIATE e espera até 10 segundos em disputa com outro processo. Conta tem @Version e o banco proíbe saldo negativo. Isso serve à demonstração local, sem reproduzir o ambiente distribuído.

Não há login, autorização, estorno nem idempotência. Cada envio cria uma nova operação. Se perder a resposta, consulte o histórico antes de repetir. A API escuta em localhost. Listas de clientes/contas/boletos não têm paginação nesta versão acadêmica.

## Defesa e testes

Leia [DEFESA.md](DEFESA.md). Execute **mvn clean verify** para os testes e o JAR. Os testes usam Spring/JPA e SQLite de verdade; verificam tipos de operação, recusas, boleto duplicado, validação HTTP, concorrência e rollback. Consulte [VALIDACAO.md](VALIDACAO.md) para o resultado real desta versão.
