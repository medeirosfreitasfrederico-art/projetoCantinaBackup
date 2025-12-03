package com.ifsp.projetoCantina.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessoes_clientes")
public class SessaoCliente {
    @Id
    private String id;
    
    @Column(name = "nome_cliente", nullable = false)
    private String nomeCliente;
    
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;
    
    @Column(name = "data_ultima_atividade", nullable = false)
    private LocalDateTime dataUltimaAtividade;
    
    @Column(nullable = false)
    private Boolean ativa = true;
    
    // Construtores
    public SessaoCliente() {}
    
    public SessaoCliente(String id, String nomeCliente) {
        this.id = id;
        this.nomeCliente = nomeCliente;
        this.dataCriacao = LocalDateTime.now();
        this.dataUltimaAtividade = LocalDateTime.now();
    }
    
    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }
    
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    
    public LocalDateTime getDataUltimaAtividade() { return dataUltimaAtividade; }
    public void setDataUltimaAtividade(LocalDateTime dataUltimaAtividade) { this.dataUltimaAtividade = dataUltimaAtividade; }
    
    public Boolean getAtiva() { return ativa; }
    public void setAtiva(Boolean ativa) { this.ativa = ativa; }
}
