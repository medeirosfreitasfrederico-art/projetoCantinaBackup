package com.ifsp.projetoCantina.controller;

import com.ifsp.projetoCantina.model.*;
import com.ifsp.projetoCantina.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/sessao")
@CrossOrigin(origins = "*")
public class SessaoController {

    @Autowired
    private SessaoClienteRepository sessaoRepository;
    
    @Autowired
    private CarrinhoRepository carrinhoRepository;
    
    @Autowired
    private ProdutoRepository produtoRepository;

    // Iniciar nova sessão
    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciarSessao(@RequestBody SessaoRequest request) {
        try {
            // Gerar ID único para a sessão
            String sessaoId = "sessao_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
            
            // Criar nova sessão
            SessaoCliente sessao = new SessaoCliente(sessaoId, request.getNomeCliente());
            sessaoRepository.save(sessao);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessaoId", sessaoId);
            response.put("message", "Sessão iniciada com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Erro ao iniciar sessão: " + e.getMessage()
            ));
        }
    }

    // Adicionar item ao carrinho
    @PostMapping("/carrinho/adicionar")
    public ResponseEntity<?> adicionarAoCarrinho(@RequestBody CarrinhoRequest request) {
        try {
            // Verificar se sessão existe e está ativa
            Optional<SessaoCliente> sessao = sessaoRepository.findByIdAndAtivaTrue(request.getSessaoId());
            if (!sessao.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Sessão inválida ou expirada"
                ));
            }
            
            // Verificar se produto existe
            Optional<Produto> produto = produtoRepository.findById(request.getProdutoId());
            if (!produto.isPresent() || !produto.get().getAtivo()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Produto não encontrado"
                ));
            }
            
            // Verificar se item já existe no carrinho
            Optional<Carrinho> itemExistente = carrinhoRepository.findBySessaoIdAndProdutoId(
                request.getSessaoId(), request.getProdutoId());
            
            if (itemExistente.isPresent()) {
                // Atualizar quantidade
                Carrinho item = itemExistente.get();
                item.setQuantidade(item.getQuantidade() + request.getQuantidade());
                carrinhoRepository.save(item);
            } else {
                // Adicionar novo item
                Carrinho novoItem = new Carrinho(request.getSessaoId(), produto.get(), request.getQuantidade());
                carrinhoRepository.save(novoItem);
            }
            
            // Atualizar atividade da sessão
            sessaoRepository.atualizarAtividade(request.getSessaoId(), LocalDateTime.now());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Item adicionado ao carrinho"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Erro ao adicionar item: " + e.getMessage()
            ));
        }
    }

    // Obter carrinho
    @GetMapping("/carrinho/{sessaoId}")
    public ResponseEntity<?> obterCarrinho(@PathVariable String sessaoId) {
        try {
            // Verificar sessão
            Optional<SessaoCliente> sessao = sessaoRepository.findByIdAndAtivaTrue(sessaoId);
            if (!sessao.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Sessão inválida"
                ));
            }
            
            // Buscar itens do carrinho
            List<Carrinho> itens = carrinhoRepository.findBySessaoId(sessaoId);
            
            // Calcular total
            double total = itens.stream()
                .mapToDouble(item -> item.getProduto().getPreco().doubleValue() * item.getQuantidade())
                .sum();
            
            // Atualizar atividade
            sessaoRepository.atualizarAtividade(sessaoId, LocalDateTime.now());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("itens", itens);
            response.put("total", total);
            response.put("cliente", sessao.get().getNomeCliente());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Erro ao carregar carrinho: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/carrinho/{sessaoId}")
public ResponseEntity<?> limparCarrinho(@PathVariable String sessaoId) {
    try {
        // Verificar se sessão existe
        Optional<SessaoCliente> sessao = sessaoRepository.findById(sessaoId);
        if (!sessao.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Sessão não encontrada"
            ));
        }
        
        // Limpar carrinho
        carrinhoRepository.limparCarrinho(sessaoId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Carrinho limpo com sucesso"
        ));
        
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "message", "Erro ao limpar carrinho: " + e.getMessage()
        ));
    }
}

// Finalizar sessão (limpa carrinho e desativa sessão)
@PostMapping("/finalizar/{sessaoId}")
public ResponseEntity<?> finalizarSessao(@PathVariable String sessaoId) {
    try {
        Optional<SessaoCliente> sessao = sessaoRepository.findById(sessaoId);
        if (sessao.isPresent()) {
            // Limpar carrinho
            carrinhoRepository.limparCarrinho(sessaoId);
            
            // Desativar sessão
            SessaoCliente sessaoObj = sessao.get();
            sessaoObj.setAtiva(false);
            sessaoRepository.save(sessaoObj);
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Sessão finalizada com sucesso"
        ));
        
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "message", "Erro ao finalizar sessão: " + e.getMessage()
        ));
    }
}

// Limpeza automática de sessões antigas (executar periodicamente)
@PostMapping("/limpeza-automatica")
public ResponseEntity<?> limpezaAutomatica() {
    try {
        LocalDateTime limite = LocalDateTime.now().minusHours(2); // Sessões com mais de 2 horas
        sessaoRepository.desativarSessoesExpiradas(limite);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Limpeza automática executada"
        ));
        
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "message", "Erro na limpeza automática: " + e.getMessage()
        ));
    }
}

    // Classes para requests
    public static class SessaoRequest {
        private String nomeCliente;
        public String getNomeCliente() { return nomeCliente; }
        public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }
    }
    
    public static class CarrinhoRequest {
        private String sessaoId;
        private Long produtoId;
        private Integer quantidade;
        
        public String getSessaoId() { return sessaoId; }
        public void setSessaoId(String sessaoId) { this.sessaoId = sessaoId; }
        public Long getProdutoId() { return produtoId; }
        public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }
        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    }
}