$ErrorActionPreference = 'Stop'
$api = 'http://127.0.0.1:8080/api'
function Enviar($rota, $dados) {
    Invoke-RestMethod -Method Post -Uri "$api/$rota" -ContentType 'application/json' -Body ($dados | ConvertTo-Json)
}

# Execute uma vez em um banco novo. Altere os dados unicos para repetir.
$ana = Enviar 'clientes' @{nome='Ana'; cpf='11111111111'}
$bia = Enviar 'clientes' @{nome='Bia'; cpf='22222222222'}
$origem = Enviar 'contas' @{clienteId=$ana.id; numero='1001'}
$destino = Enviar 'contas' @{clienteId=$bia.id; numero='1002'}
Enviar "contas/$($origem.id)/depositos" @{valorCentavos=10000} | Format-List

foreach ($tipo in @('PIX', 'TED', 'DOC')) {
    Enviar 'transferencias' @{origemId=$origem.id; destinoId=$destino.id; valorCentavos=1000; tipo=$tipo} | Format-List
}
foreach ($tipo in @('PIX', 'SALDO')) {
    Enviar 'pagamentos' @{contaId=$origem.id; valorCentavos=1000; tipo=$tipo; referencia='Loja'} | Format-List
}
$boleto = Enviar 'boletos' @{codigo='BOL-1'; valorCentavos=1000; vencimento='2026-11-30'}
Enviar "boletos/$($boleto.id)/pagamentos" @{contaId=$origem.id} | Format-List

try {
    Enviar 'pagamentos' @{contaId=$origem.id; valorCentavos=99999; tipo='PIX'; referencia='Recusa esperada'}
} catch {
    if ([int]$_.Exception.Response.StatusCode -ne 422) { throw }
    Write-Host '422 esperado: saldo insuficiente; tentativa registrada.'
}
Invoke-RestMethod "$api/contas/$($origem.id)" | Format-List
Invoke-RestMethod "$api/contas/$($destino.id)" | Format-List
Invoke-RestMethod "$api/contas/$($origem.id)/movimentacoes" | Format-Table
Write-Host 'Esperado: Ana 4000 centavos; Bia 3000 centavos; 8 movimentacoes para Ana.'
Write-Host "Reinicie o servidor e consulte $api/contas/$($origem.id) para confirmar a persistencia."
