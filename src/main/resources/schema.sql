CREATE TABLE IF NOT EXISTS clientes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    cpf TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS contas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cliente_id INTEGER NOT NULL REFERENCES clientes(id),
    numero TEXT NOT NULL UNIQUE,
    saldo_centavos INTEGER NOT NULL DEFAULT 0 CHECK (saldo_centavos >= 0),
    versao INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS boletos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    valor_centavos INTEGER NOT NULL CHECK (valor_centavos > 0),
    vencimento TEXT NOT NULL,
    pago BOOLEAN NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS movimentacoes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    origem_id INTEGER REFERENCES contas(id),
    destino_id INTEGER REFERENCES contas(id),
    boleto_id INTEGER REFERENCES boletos(id),
    natureza TEXT NOT NULL,
    tipo TEXT NOT NULL,
    valor_centavos INTEGER NOT NULL CHECK (valor_centavos > 0),
    referencia TEXT NOT NULL,
    situacao TEXT NOT NULL CHECK (situacao IN ('APROVADA', 'RECUSADA')),
    data_hora TEXT NOT NULL,
    protocolo_banco_central TEXT NOT NULL,
    CHECK (origem_id IS NOT NULL OR destino_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_mov_origem ON movimentacoes(origem_id);
CREATE INDEX IF NOT EXISTS idx_mov_destino ON movimentacoes(destino_id);
