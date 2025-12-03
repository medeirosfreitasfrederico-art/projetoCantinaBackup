package com.ifsp.projetoCantina.model;

import jakarta.persistence.*;

@Entity
@Table(name = "perguntas_seguranca")
public class PerguntaSeguranca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String pergunta;
    
    @Column(nullable = false)
    private Boolean ativa = true;
    
    // Construtores
    public PerguntaSeguranca() {}
    
    public PerguntaSeguranca(String pergunta) {
        this.pergunta = pergunta;
    }
    
    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getPergunta() { return pergunta; }
    public void setPergunta(String pergunta) { this.pergunta = pergunta; }
    
    public Boolean getAtiva() { return ativa; }
    public void setAtiva(Boolean ativa) { this.ativa = ativa; }
}
