# Dos nove diagramas para a versão simples

| Anexo | Fluxo/componente | Onde está no código |
|---|---|---|
| 01 — 222456 | Transferência TED | BancoService.transferir, tipo TED |
| 02 — 222440 | Transferência Pix | BancoService.transferir, tipo PIX |
| 03 — 222406 | Transferência DOC | BancoService.transferir, tipo DOC |
| 04 — 222343 | Pagamento Pix | BancoService.pagar, tipo PIX |
| 05 — 222329 | Consulta de saldo | consultarConta e GET /api/contas/{id}/saldo |
| 06 — 222312 | Pagamento boleto | criarBoleto e pagarBoleto |
| 07 — 222240 | Pagamento com saldo | BancoService.pagar, tipo SALDO |
| 08 — 222030 | Componentes | Responsabilidades representadas por métodos do serviço |
| 09 — 222013 | Implantação | Spring Boot/Java preservados; SQLite local e aplicação única |

As imagens originais estão em diagramas/. O diagrama de implantação é uma referência conceitual; não implementamos Kubernetes, Nginx ou PostgreSQL. Os módulos foram reunidos em um serviço, conforme o pedido de simplificação.

Cliente foi acrescentado para cadastro explícito do titular, seguindo a organização do exemplo dos colegas. Boleto agora possui tabela própria. Histórico e comunicação simulada compartilham Movimentacao. Pagamento Pix/SALDO representa beneficiário externo pela referencia; o dinheiro sai do conjunto de contas locais. Transferência exige duas contas locais e conserva a soma dos saldos.

As relações usam IDs e foreign keys no SQLite, sem coleções JPA. Não há tabelas por modalidade. A comunicação é fictícia e transacional; não reproduz atomicidade com um serviço externo. Recusas por saldo são persistidas; erros de entrada não. Falhas técnicas revertem a operação inteira.
