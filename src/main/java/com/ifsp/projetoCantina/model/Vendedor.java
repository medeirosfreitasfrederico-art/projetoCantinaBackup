package com.ifsp.projetoCantina.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vendedores")
public class Vendedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String usuario;
    
    @Column(nullable = false)
    private String senha;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = true)
    private String telefone;
    
    @Column(unique = true, nullable = false)
    private String cpf;
    
    private String nome;
    
    @Column(name = "pergunta_seguranca_1", nullable = false)
    private String perguntaSeguranca1;
    
    @Column(name = "resposta_seguranca_1", nullable = false)
    private String respostaSeguranca1;
    
    @Column(name = "pergunta_seguranca_2", nullable = false)
    private String perguntaSeguranca2;
    
    @Column(name = "resposta_seguranca_2", nullable = false)
    private String respostaSeguranca2;

    @Enumerated(EnumType.STRING)
    @Column(name = "validado", nullable = false)
    private Validado validado = Validado.NÃO;
    

    // Construtores
    public Vendedor() {}
    
    public Vendedor(String usuario, String senha, String nome, String cpf) {
        this.usuario = usuario;
        this.senha = senha;
        this.nome = nome;
        this.cpf = cpf;
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getPerguntaSeguranca1() { return perguntaSeguranca1; }
    public void setPerguntaSeguranca1(String perguntaSeguranca1) { this.perguntaSeguranca1 = perguntaSeguranca1; }
    
    public String getRespostaSeguranca1() { return respostaSeguranca1; }
    public void setRespostaSeguranca1(String respostaSeguranca1) { this.respostaSeguranca1 = respostaSeguranca1; }
    
    public String getPerguntaSeguranca2() { return perguntaSeguranca2; }
    public void setPerguntaSeguranca2(String perguntaSeguranca2) { this.perguntaSeguranca2 = perguntaSeguranca2; }
    
    public String getRespostaSeguranca2() { return respostaSeguranca2; }
    public void setRespostaSeguranca2(String respostaSeguranca2) { this.respostaSeguranca2 = respostaSeguranca2; }

    public Validado getValidado() { return validado; }
    public void setValidado(Validado validado) { this.validado = validado; }

        // Enum para validação
        public enum Validado {
            SIM("sim"), NÃO("não");
        
            private final String valorBanco;
        
            Validado(String valorBanco) {
                this.valorBanco = valorBanco;
            }
        
            public String getValorBanco() {
                return valorBanco;
            }
        
            // Método para converter do banco para enum
            public static Validado fromString(String value) {
                if (value == null) return null;
                
                for (Validado validado : Validado.values()) {
                    if (validado.valorBanco.equalsIgnoreCase(value)) {
                        return validado;
                    }
                }
                throw new IllegalArgumentException("Valor inválido para Validado: " + value);
            }
        }
}


