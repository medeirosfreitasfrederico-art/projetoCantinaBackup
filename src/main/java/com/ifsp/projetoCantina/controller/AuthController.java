package com.ifsp.projetoCantina.controller;  

import com.ifsp.projetoCantina.model.Vendedor;
import com.ifsp.projetoCantina.repository.VendedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private VendedorRepository vendedorRepository;
    
    // Remova este método não utilizado ou implemente-o corretamente

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Optional<Vendedor> vendedor = vendedorRepository.findByUsuarioAndSenha(
                loginRequest.getUsuario(),
                loginRequest.getSenha()
            );
    
            Map<String, Object> response = new HashMap<>();
    
            if (vendedor.isPresent()) {
                // ✅ VERIFICAR SE A CONTA ESTÁ VALIDADA
                if (vendedor.get().getValidado() != Vendedor.Validado.SIM) {
                    response.put("success", false);
                    response.put("message", "Conta aguardando validação do administrador");
                    return ResponseEntity.status(403).body(response);
                }
                
                response.put("success", true);
                response.put("message", "Login bem-sucedido");
                response.put("vendedor", vendedor.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Usuário ou senha incorretos");
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    public static class LoginRequest {
        private String usuario;
        private String senha;

        // Getters e Setters
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getSenha() { return senha; }
        public void setSenha(String senha) { this.senha = senha; }
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrarVendedor(@RequestBody CadastroRequest cadastroRequest) {
        try {
            // Verificar se usuário já existe
            if (vendedorRepository.findByUsuario(cadastroRequest.getUsuario()).isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Usuário já existe");
                return ResponseEntity.badRequest().body(response);  
            }       
            
            // Verificar se email já existe
            if (vendedorRepository.findByEmail(cadastroRequest.getEmail()).isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "E-mail já cadastrado");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Verificar se CPF já existe
            if (vendedorRepository.findByCpf(cadastroRequest.getCpf()).isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "CPF já cadastrado");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Criar novo vendedor
            Vendedor novoVendedor = new Vendedor();
            novoVendedor.setUsuario(cadastroRequest.getUsuario());
            novoVendedor.setSenha(cadastroRequest.getSenha());
            novoVendedor.setNome(cadastroRequest.getNome());
            novoVendedor.setEmail(cadastroRequest.getEmail());
            novoVendedor.setTelefone(cadastroRequest.getTelefone());
            novoVendedor.setCpf(cadastroRequest.getCpf());

            novoVendedor.setValidado(Vendedor.Validado.NÃO);
            
            // Salvar perguntas de segurança diretamente no vendedor
            if (cadastroRequest.getPerguntasSeguranca() != null && 
                cadastroRequest.getPerguntasSeguranca().size() >= 2) {
                
                novoVendedor.setPerguntaSeguranca1(cadastroRequest.getPerguntasSeguranca().get(0).getPergunta());
                novoVendedor.setRespostaSeguranca1(cadastroRequest.getPerguntasSeguranca().get(0).getResposta());
                novoVendedor.setPerguntaSeguranca2(cadastroRequest.getPerguntasSeguranca().get(1).getPergunta());
                novoVendedor.setRespostaSeguranca2(cadastroRequest.getPerguntasSeguranca().get(1).getResposta());
            }
            
            vendedorRepository.save(novoVendedor);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cadastro realizado com sucesso! Aguarde validação do administrador.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    // Método para buscar perguntas de um vendedor específico
    private List<String> buscarPerguntasPorVendedor(Long vendedorId) {
        List<String> perguntas = new ArrayList<>();
        Optional<Vendedor> vendedor = vendedorRepository.findById(vendedorId);
        
        if (vendedor.isPresent()) {
            perguntas.add(vendedor.get().getPerguntaSeguranca1());
            perguntas.add(vendedor.get().getPerguntaSeguranca2());
        }
        
        return perguntas;
    }
    
    // Método para verificar respostas no banco de dados
    private boolean verificarRespostasNoBanco(Long vendedorId, Map<String, String> respostas) {
        Optional<Vendedor> vendedor = vendedorRepository.findById(vendedorId);
        
        if (!vendedor.isPresent()) {
            return false;
        }
        
        Vendedor v = vendedor.get();
        
        // Verificar se as respostas fornecidas correspondem às do banco
        int respostasCorretas = 0;
        
        for (Map.Entry<String, String> entry : respostas.entrySet()) {
            String pergunta = entry.getKey();
            String respostaUsuario = entry.getValue();
            
            if (pergunta.equals(v.getPerguntaSeguranca1()) && 
                respostaUsuario.equalsIgnoreCase(v.getRespostaSeguranca1())) {
                respostasCorretas++;
            }
            else if (pergunta.equals(v.getPerguntaSeguranca2()) && 
                     respostaUsuario.equalsIgnoreCase(v.getRespostaSeguranca2())) {
                respostasCorretas++;
            }
        }
        
        return respostasCorretas >= 2; // Pelo menos 2 respostas corretas
    }

    // Classe para receber os dados de cadastro
    public static class CadastroRequest {
        private String usuario;
        private String senha;
        private String nome;
        private String email;
        private String telefone;
        private String cpf;
        private List<PerguntaSeguranca> perguntasSeguranca;
        
        // Getters e Setters
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
        public List<PerguntaSeguranca> getPerguntasSeguranca() { return perguntasSeguranca; }
        public void setPerguntasSeguranca(List<PerguntaSeguranca> perguntasSeguranca) { this.perguntasSeguranca = perguntasSeguranca; }
    }

    public static class PerguntaSeguranca {
        private String pergunta;
        private String resposta;
        
        // Getters e Setters
        public String getPergunta() { return pergunta; }
        public void setPergunta(String pergunta) { this.pergunta = pergunta; }
        public String getResposta() { return resposta; }
        public void setResposta(String resposta) { this.resposta = resposta; }
    }

    // Endpoint para verificar dados de recuperação
    @PostMapping("/verificar-dados-recuperacao")
    public ResponseEntity<?> verificarDadosRecuperacao(@RequestBody VerificacaoRequest verificacaoRequest) {
        try {
            Optional<Vendedor> vendedor = vendedorRepository.findByUsuario(verificacaoRequest.getUsuario());
            
            if (!vendedor.isPresent() || 
                !vendedor.get().getEmail().equals(verificacaoRequest.getEmail()) ||
                !vendedor.get().getCpf().equals(verificacaoRequest.getCpf())) {
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Dados não correspondem");
                return ResponseEntity.status(401).body(response);
            }
            
            // Buscar perguntas de segurança do vendedor específico
            List<String> perguntas = Arrays.asList(
                vendedor.get().getPerguntaSeguranca1(),
                vendedor.get().getPerguntaSeguranca2()
            );
            
            // Guardar o ID do vendedor na sessão ou no token para uso posterior
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("perguntas", perguntas);
            response.put("vendedorId", vendedor.get().getId()); // Enviar o ID para o frontend
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Endpoint para verificar respostas de segurança
    @PostMapping("/verificar-respostas")
    public ResponseEntity<?> verificarRespostas(@RequestBody RespostasRequest respostasRequest) {
        try {
            Optional<Vendedor> vendedor = vendedorRepository.findByUsuario(respostasRequest.getUsuario());
            
            if (!vendedor.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Usuário não encontrado");
                return ResponseEntity.status(401).body(response);
            }
            
            // Verificar respostas no banco de dados
            boolean respostasCorretas = verificarRespostasNoBanco(
                vendedor.get().getId(), 
                respostasRequest.getRespostas()
            );
            
            if (!respostasCorretas) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Respostas incorretas");
                return ResponseEntity.status(401).body(response);
            }
            
            // Gerar token de redefinição (simplificado - não estamos usando tokens no momento)
            String token = "token_simulado_" + System.currentTimeMillis();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("vendedorId", vendedor.get().getId()); // Enviar o ID para o frontend
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
        
    // Endpoint para redefinir senha
    @PostMapping("/redefinir-senha")
    public ResponseEntity<?> redefinirSenha(@RequestBody RedefinicaoSenhaRequest redefinicaoSenhaRequest) {
        try {
            // Buscar vendedor pelo usuário diretamente
            Optional<Vendedor> vendedor = vendedorRepository.findByUsuario(redefinicaoSenhaRequest.getUsuario());
            
            if (!vendedor.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Usuário não encontrado");
                return ResponseEntity.status(401).body(response);
            }
            
            // Atualizar senha
            vendedor.get().setSenha(redefinicaoSenhaRequest.getNovaSenha());
            vendedorRepository.save(vendedor.get());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Senha redefinida com sucesso");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Classes de request para recuperação de senha
    public static class VerificacaoRequest {
        private String usuario;
        private String email;
        private String cpf;
        
        // Getters e Setters
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCpf() { return cpf; }
        public void setCpf(String cpf) { this.cpf = cpf; }
    }

    public static class RespostasRequest {
        private String usuario;
        private Map<String, String> respostas;
        
        // Getters e Setters
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public Map<String, String> getRespostas() { return respostas; }
        public void setRespostas(Map<String, String> respostas) { this.respostas = respostas; }
    }

    public static class RedefinicaoSenhaRequest {
        private String usuario;
        private String novaSenha;
        
        // Getters e Setters
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getNovaSenha() { return novaSenha; }
        public void setNovaSenha(String novaSenha) { this.novaSenha = novaSenha; }
    }
}