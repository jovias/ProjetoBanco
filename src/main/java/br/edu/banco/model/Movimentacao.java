package br.edu.banco.model;

import jakarta.persistence.*;

@Entity
@Table(name = "movimentacoes")
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long origemId;
    private Long destinoId;
    private Long boletoId;
    private String natureza;
    private String tipo;
    private long valorCentavos;
    private String referencia;
    private String situacao;
    private String dataHora;
    private String protocoloBancoCentral;

    public Movimentacao() {
    }

    public Long getId() {
        return id;
    }

    public Long getOrigemId() {
        return origemId;
    }

    public void setOrigemId(Long origemId) {
        this.origemId = origemId;
    }

    public Long getDestinoId() {
        return destinoId;
    }

    public void setDestinoId(Long destinoId) {
        this.destinoId = destinoId;
    }

    public Long getBoletoId() {
        return boletoId;
    }

    public void setBoletoId(Long boletoId) {
        this.boletoId = boletoId;
    }

    public String getNatureza() {
        return natureza;
    }

    public void setNatureza(String natureza) {
        this.natureza = natureza;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public void setValorCentavos(long valorCentavos) {
        this.valorCentavos = valorCentavos;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }

    public String getProtocoloBancoCentral() {
        return protocoloBancoCentral;
    }

    public void setProtocoloBancoCentral(String protocoloBancoCentral) {
        this.protocoloBancoCentral = protocoloBancoCentral;
    }
}
