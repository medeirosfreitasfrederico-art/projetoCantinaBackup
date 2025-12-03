package com.ifsp.projetoCantina.controller;

import com.ifsp.projetoCantina.model.Produto;
import com.ifsp.projetoCantina.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/produtos")
@CrossOrigin(origins = "*")
public class ProdutoController {

    @Autowired
    private ProdutoRepository produtoRepository;

    private final String UPLOAD_DIR = "uploads/";

    // Listar todos os produtos ativos (para clientes)
    @GetMapping
    public List<Produto> listarProdutosAtivos() {
        return produtoRepository.findByAtivoTrueOrderByNome();
    }

    // Buscar produto por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarProduto(@PathVariable Long id) {
        try {
            Optional<Produto> produto = produtoRepository.findById(id);
            
            if (produto.isPresent()) {
                return ResponseEntity.ok(produto.get());
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Produto não encontrado");
                return ResponseEntity.status(404).body(response);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Criar novo produto com upload de imagem
    @PostMapping
    public ResponseEntity<?> criarProduto(@RequestParam("nome") String nome,
                                          @RequestParam("descricao") String descricao,
                                          @RequestParam("preco") BigDecimal preco,
                                          @RequestParam("estoque") Integer estoque,
                                          @RequestParam(value = "imagem", required = false) MultipartFile imagem) {
        try {
            Optional<Produto> produtoExistente = produtoRepository.findByNome(nome);
            if (produtoExistente.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Já existe um produto com este nome");
                return ResponseEntity.badRequest().body(response);
            }

            Produto novoProduto = new Produto();
            novoProduto.setNome(nome);
            novoProduto.setDescricao(descricao);
            novoProduto.setPreco(preco);
            novoProduto.setEstoque(estoque);
            novoProduto.setAtivo(true);

            if (imagem != null && !imagem.isEmpty()) {
                String nomeArquivoUnico = UUID.randomUUID().toString() + "_" + imagem.getOriginalFilename();
                Path caminhoDestino = Paths.get(UPLOAD_DIR + nomeArquivoUnico);
                Files.createDirectories(Paths.get(UPLOAD_DIR));
                Files.copy(imagem.getInputStream(), caminhoDestino, StandardCopyOption.REPLACE_EXISTING);
                novoProduto.setImagem(nomeArquivoUnico);
            }

            produtoRepository.save(novoProduto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produto criado com sucesso");
            response.put("produto", novoProduto);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Atualizar produto com upload de imagem
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarProduto(@PathVariable Long id,
                                              @RequestParam("nome") String nome,
                                              @RequestParam("descricao") String descricao,
                                              @RequestParam("preco") BigDecimal preco,
                                              @RequestParam("estoque") Integer estoque,
                                              @RequestParam(value = "imagem", required = false) MultipartFile imagem) {
        try {
            Optional<Produto> produtoOpt = produtoRepository.findById(id);
            
            if (!produtoOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Produto não encontrado");
                return ResponseEntity.status(404).body(response);
            }
    
            Produto produtoAtualizado = produtoOpt.get();
            produtoAtualizado.setNome(nome);
            produtoAtualizado.setDescricao(descricao);
            produtoAtualizado.setPreco(preco);
            produtoAtualizado.setEstoque(estoque);

            if (imagem != null && !imagem.isEmpty()) {
                if (produtoAtualizado.getImagem() != null && !produtoAtualizado.getImagem().isEmpty()) {
                    Files.deleteIfExists(Paths.get(UPLOAD_DIR + produtoAtualizado.getImagem()));
                }
                
                String nomeArquivoUnico = UUID.randomUUID().toString() + "_" + imagem.getOriginalFilename();
                Path caminhoDestino = Paths.get(UPLOAD_DIR + nomeArquivoUnico);
                Files.copy(imagem.getInputStream(), caminhoDestino, StandardCopyOption.REPLACE_EXISTING);
                produtoAtualizado.setImagem(nomeArquivoUnico);
            }
    
            produtoRepository.save(produtoAtualizado);
    
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produto atualizado com sucesso");
            response.put("produto", produtoAtualizado);
            return ResponseEntity.ok(response);
    
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Desativar produto (delete lógico)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> desativarProduto(@PathVariable Long id) {
        try {
            Optional<Produto> produto = produtoRepository.findById(id);
            
            if (!produto.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Produto não encontrado");
                return ResponseEntity.status(404).body(response);
            }

            Produto produtoDesativado = produto.get();
            produtoDesativado.setAtivo(false);
            produtoRepository.save(produtoDesativado);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Produto desativado com sucesso");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // A classe ProdutoRequest não é mais usada ativamente para POST e PUT, mas mantida para não quebrar outras partes do código.
    public static class ProdutoRequest {
        private String nome;
        private String descricao;
        private BigDecimal preco;
        private Integer estoque;

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public BigDecimal getPreco() { return preco; }
        public void setPreco(BigDecimal preco) { this.preco = preco; }
        public Integer getEstoque() { return estoque; }
        public void setEstoque(Integer estoque) { this.estoque = estoque; }
    }
}