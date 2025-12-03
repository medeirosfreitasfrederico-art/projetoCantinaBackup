package com.ifsp.projetoCantina.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vendedor_perguntas")
public class VendedorPergunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Vendedor vendedor;
    
    @ManyToOne
    @JoinColumn(name = "pergunta_id", nullable = false)
    private PerguntaSeguranca pergunta;
    
    @Column(nullable = false)
    private String resposta;
    
    // Construtores
    public VendedorPergunta() {}
    
    public VendedorPergunta(Vendedor vendedor, PerguntaSeguranca pergunta, String resposta) {
        this.vendedor = vendedor;
        this.pergunta = pergunta;
        this.resposta = resposta;
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Vendedor getVendedor() { return vendedor; }
    public void setVendedor(Vendedor vendedor) { this.vendedor = vendedor; }
    
    public PerguntaSeguranca getPergunta() { return pergunta; }
    public void setPergunta(PerguntaSeguranca pergunta) { this.pergunta = pergunta; }
    
    public String getResposta() { return resposta; }
    public void setResposta(String resposta) { this.resposta = resposta; }
}