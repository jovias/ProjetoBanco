# Validação da versão simples

Executada em 13/11/2026.

**mvn clean verify: BUILD SUCCESS.**

**15 testes executados; 0 falhas, 0 erros, 0 ignorados.**

| Classe | Execuções | O que verifica |
|---|---:|---|
| BancoServiceTest | 13 | Pix/TED/DOC, pagamentos Pix/SALDO, boleto e pagamento duplicado, recusas, saldo exato, dados repetidos, validação HTTP, centavos fracionados e concorrência |
| PersistenciaTest | 1 | Fecha o contexto Spring, reabre o mesmo arquivo SQLite e confirma saldo, histórico e protocolo |
| RollbackTest | 1 | Falha simulada reverte transferência e pagamento, mantendo o boleto aberto |

O teste de persistência deposita 10000 centavos, paga 550 por Pix, encerra o contexto e reabre o arquivo. Confirma saldo de **9450 centavos**, duas movimentações e protocolo SIM-. Não é um teste em H2.

O teste concorrente disputa 10000 centavos com duas transferências de 8000: uma é aprovada e outra recusada; saldos finais 2000 e 8000.

Ambiente: Windows, Oracle JDK 26.0.1, Maven 3.9.11, compilação com **release 21**, Spring Boot 3.5.7 e SQLite JDBC 3.50.3.0. Não foi executada uma segunda build em JDK 21. Foram usadas dependências já disponíveis localmente, sem novos downloads.

Os testes exercitam HTTP com MockMvc e persistência real. Nesta revisão não foi iniciado um servidor HTTP externo nem executado o script PowerShell de demonstração. A documentação informa o procedimento para rodá-lo no IntelliJ. Não há implementação ou teste de Banco Central real.

Relatórios resumidos do Maven: pasta relatorios/.
