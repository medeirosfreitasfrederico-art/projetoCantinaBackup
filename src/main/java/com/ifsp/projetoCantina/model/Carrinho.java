package com.ifsp.projetoCantina.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "carrinhos")
public class Carrinho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "sessao_id", nullable = false)
    private String sessaoId;
    
    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;
    
    @Column(nullable = false)
    private Integer quantidade;
    
    @Column(name = "data_adicao", nullable = false)
    private LocalDateTime dataAdicao;
    
    // Construtores
    public Carrinho() {}
    
    public Carrinho(String sessaoId, Produto produto, Integer quantidade) {
        this.sessaoId = sessaoId;
        this.produto = produto;
        this.quantidade = quantidade;
        this.dataAdicao = LocalDateTime.now();
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSessaoId() { return sessaoId; }
    public void setSessaoId(String sessaoId) { this.sessaoId = sessaoId; }
    
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    
    public LocalDateTime getDataAdicao() { return dataAdicao; }
    public void setDataAdicao(LocalDateTime dataAdicao) { this.dataAdicao = dataAdicao; }
}