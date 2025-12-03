package com.ifsp.projetoCantina.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String codigo;
    
    @Column(name = "cliente_nome", nullable = false)
    private String clienteNome;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPedido status = StatusPedido.PENDENTE;
    
    @Column(name = "metodo_pagamento")
    private String metodoPagamento;
    
    @Column(name = "data_pedido", nullable = false)
    private LocalDateTime dataPedido;
    
    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;
    
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PedidoItem> itens;

    //-------------------------------------
    // Adicione este campo na classe Pedido
@Enumerated(EnumType.STRING)
@Column(name = "status_pagamento")
private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

// Getters e Setters
public StatusPagamento getStatusPagamento() { return statusPagamento; }
public void setStatusPagamento(StatusPagamento statusPagamento) { this.statusPagamento = statusPagamento; }

// Enum
public enum StatusPagamento {
    PENDENTE, APROVADO, CANCELADO
}
//-------------------------------------

//------------------------------------------------------------------------------------
// Adicionar este método para conversão case-insensitive
@PostLoad
public void postLoad() {
    if (this.status != null) {
        try {
            // Tenta converter para maiúsculo se estiver em minúsculo
            this.status = StatusPedido.valueOf(this.status.name().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Mantém o valor atual se não conseguir converter
        }
    }
}
    
    // Construtores
    public Pedido() {
        this.dataPedido = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    public Pedido(String codigo, String clienteNome, BigDecimal total) {
        this();
        this.codigo = codigo;
        this.clienteNome = clienteNome;
        this.total = total;
    }
//------------------------------------------------------------------------------------
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    
    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    
    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { 
        this.status = status; 
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    
    public LocalDateTime getDataPedido() { return dataPedido; }
    public void setDataPedido(LocalDateTime dataPedido) { this.dataPedido = dataPedido; }
    
    public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
    
    public List<PedidoItem> getItens() { return itens; }
    public void setItens(List<PedidoItem> itens) { this.itens = itens; }
}