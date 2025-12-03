package com.ifsp.projetoCantina.controller;

import com.ifsp.projetoCantina.model.*;
import com.ifsp.projetoCantina.repository.PedidoRepository;
import com.ifsp.projetoCantina.repository.ProdutoRepository;
import com.ifsp.projetoCantina.repository.PedidoItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*")
public class PedidoController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private PedidoItemRepository pedidoItemRepository;

    // Listar todos os pedidos (para vendedores)
@GetMapping
public ResponseEntity<?> listarPedidos() {
    try {
        List<Pedido> pedidos = pedidoRepository.findAllByOrderByDataPedidoDesc();
         // Ordenar manualmente para garantir a ordem correta
         pedidos.sort((a, b) -> {
            // Pedidos PENDENTE e PREPARANDO têm prioridade
            if (a.getStatus() == StatusPedido.PENDENTE && b.getStatus() != StatusPedido.PENDENTE) return -1;
            if (a.getStatus() == StatusPedido.PREPARANDO && 
                (b.getStatus() != StatusPedido.PENDENTE && b.getStatus() != StatusPedido.PREPARANDO)) return -1;
            if (b.getStatus() == StatusPedido.PENDENTE && a.getStatus() != StatusPedido.PENDENTE) return 1;
            if (b.getStatus() == StatusPedido.PREPARANDO && 
                (a.getStatus() != StatusPedido.PENDENTE && a.getStatus() != StatusPedido.PREPARANDO)) return 1;
            
            // Para mesmo status, ordenar por data (mais recente primeiro)
            return b.getDataPedido().compareTo(a.getDataPedido());
        });
        
        // Criar DTOs para evitar problemas de serialização
        List<Map<String, Object>> pedidosResponse = new ArrayList<>();
        
        for (Pedido pedido : pedidos) {
            Map<String, Object> pedidoMap = new HashMap<>();
            pedidoMap.put("id", pedido.getId());
            pedidoMap.put("codigo", pedido.getCodigo());
            pedidoMap.put("clienteNome", pedido.getClienteNome());
            pedidoMap.put("total", pedido.getTotal());
            pedidoMap.put("status", pedido.getStatus().toString());
            pedidoMap.put("metodoPagamento", pedido.getMetodoPagamento());
            pedidoMap.put("dataPedido", pedido.getDataPedido());
            pedidoMap.put("dataAtualizacao", pedido.getDataAtualizacao());
            
            // Carregar itens do pedido
            List<PedidoItem> itens = pedidoItemRepository.findByPedidoId(pedido.getId());
            List<Map<String, Object>> itensMap = new ArrayList<>();
            
            for (PedidoItem item : itens) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("quantidade", item.getQuantidade());
                itemMap.put("precoUnitario", item.getPrecoUnitario());
                
                // Informações básicas do produto
                if (item.getProduto() != null) {
                    Map<String, Object> produtoMap = new HashMap<>();
                    produtoMap.put("id", item.getProduto().getId());
                    produtoMap.put("nome", item.getProduto().getNome());
                    produtoMap.put("preco", item.getProduto().getPreco());
                    produtoMap.put("descricao", item.getProduto().getDescricao());
                    itemMap.put("produto", produtoMap);
                }
                
                itensMap.add(itemMap);
            }
            
            pedidoMap.put("itens", itensMap);
            pedidosResponse.add(pedidoMap);
        }
        
        return ResponseEntity.ok(pedidosResponse);
        
    } catch (Exception e) {
        e.printStackTrace(); // Para debug no console do Spring Boot
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Erro ao carregar pedidos: " + e.getMessage());
        return ResponseEntity.status(500).body(errorResponse);
    }
}

    // Listar pedidos por status
    @GetMapping("/status/{status}")
    public List<Pedido> listarPedidosPorStatus(@PathVariable StatusPedido status) {
        return pedidoRepository.findByStatusOrderByDataPedidoAsc(status);
    }

    // Buscar pedido por código
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<?> buscarPedidoPorCodigo(@PathVariable String codigo) {
        Optional<Pedido> pedido = pedidoRepository.findByCodigo(codigo);
        
        if (pedido.isPresent()) {
            return ResponseEntity.ok(pedido.get());
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Pedido não encontrado");
            return ResponseEntity.status(404).body(response);
        }
    }

    // Criar novo pedido
    @PostMapping
    public ResponseEntity<?> criarPedido(@RequestBody PedidoRequest pedidoRequest) {
        try {
            // Validar itens do pedido
            if (pedidoRequest.getItens() == null || pedidoRequest.getItens().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Pedido deve conter pelo menos um item");
                return ResponseEntity.badRequest().body(response);
            }

            // Gerar código único do pedido
            String codigoPedido = gerarCodigoPedido();

            // Calcular total e validar produtos
            BigDecimal total = BigDecimal.ZERO;
            List<PedidoItem> itens = new ArrayList<>();

            for (ItemPedidoRequest itemReq : pedidoRequest.getItens()) {
                Optional<Produto> produto = produtoRepository.findById(itemReq.getProdutoId());
                
                if (!produto.isPresent() || !produto.get().getAtivo()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Produto não encontrado: " + itemReq.getProdutoId());
                    return ResponseEntity.badRequest().body(response);
                }

                if (produto.get().getEstoque() < itemReq.getQuantidade()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("message", "Estoque insuficiente para: " + produto.get().getNome());
                    return ResponseEntity.badRequest().body(response);
                }

                Produto prod = produto.get();
                BigDecimal precoUnitario = prod.getPreco();
                BigDecimal totalItem = precoUnitario.multiply(BigDecimal.valueOf(itemReq.getQuantidade()));
                total = total.add(totalItem);
            }

            // Criar pedido
            Pedido novoPedido = new Pedido();
            novoPedido.setCodigo(codigoPedido);
            novoPedido.setClienteNome(pedidoRequest.getClienteNome());
            novoPedido.setTotal(total);
            novoPedido.setMetodoPagamento(pedidoRequest.getMetodoPagamento());
            novoPedido.setStatus(StatusPedido.PENDENTE);

            Pedido pedidoSalvo = pedidoRepository.save(novoPedido);

            // Criar itens do pedido e atualizar estoque
            for (ItemPedidoRequest itemReq : pedidoRequest.getItens()) {
                Produto produto = produtoRepository.findById(itemReq.getProdutoId()).get();
                BigDecimal precoUnitario = produto.getPreco();

                PedidoItem item = new PedidoItem();
                item.setPedido(pedidoSalvo);
                item.setProduto(produto);
                item.setQuantidade(itemReq.getQuantidade());
                item.setPrecoUnitario(precoUnitario);

                pedidoItemRepository.save(item);

                // Atualizar estoque
                produto.setEstoque(produto.getEstoque() - itemReq.getQuantidade());
                produtoRepository.save(produto);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Pedido criado com sucesso");
            response.put("pedido", pedidoSalvo);
            response.put("codigo", codigoPedido);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------
    @GetMapping("/{id}")
public ResponseEntity<?> buscarPedido(@PathVariable Long id) {
    try {
        Optional<Pedido> pedido = pedidoRepository.findById(id);
        
        if (pedido.isPresent()) {
            return ResponseEntity.ok(pedido.get());
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Pedido não encontrado");
            return ResponseEntity.status(404).body(response);
        }
    } catch (Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
        return ResponseEntity.status(500).body(errorResponse);
    }
}

@PutMapping("/{id}/status")
public ResponseEntity<?> atualizarStatus(@PathVariable Long id, @RequestBody StatusRequest statusRequest) {
    try {
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(id);
        
        if (!pedidoOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Pedido não encontrado");
            return ResponseEntity.status(404).body(response);
        }

        Pedido pedido = pedidoOpt.get();
        StatusPedido statusAnterior = pedido.getStatus();
        pedido.setStatus(statusRequest.getStatus());
        
        // Se o pedido foi cancelado, devolver estoque
        if (statusRequest.getStatus() == StatusPedido.CANCELADO && 
            statusAnterior != StatusPedido.CANCELADO) {
            devolverEstoque(pedido);
        }
        
        Pedido pedidoAtualizado = pedidoRepository.save(pedido);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Status do pedido atualizado com sucesso");
        response.put("pedido", pedidoAtualizado);
        return ResponseEntity.ok(response);

    } catch (Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Erro interno do servidor: " + e.getMessage());
        return ResponseEntity.status(500).body(errorResponse);
    }
}


    // ------------------------------------------------------------------------------------------------------------------------------------------

    // Método para devolver estoque quando um pedido é cancelado
    private void devolverEstoque(Pedido pedido) {
        List<PedidoItem> itens = pedidoItemRepository.findByPedidoId(pedido.getId());
        for (PedidoItem item : itens) {
            Produto produto = item.getProduto();
            produto.setEstoque(produto.getEstoque() + item.getQuantidade());
            produtoRepository.save(produto);
        }
    }

    // Estatísticas dos pedidos
    @GetMapping("/estatisticas")
    public Map<String, Object> getEstatisticas() {
        Map<String, Object> estatisticas = new HashMap<>();
        
        estatisticas.put("totalPedidos", pedidoRepository.count());
        estatisticas.put("pedidosPendentes", pedidoRepository.countByStatus(StatusPedido.PENDENTE));
        estatisticas.put("pedidosPreparando", pedidoRepository.countByStatus(StatusPedido.PREPARANDO));
        estatisticas.put("pedidosProntos", pedidoRepository.countByStatus(StatusPedido.PRONTO));
        estatisticas.put("pedidosEntregues", pedidoRepository.countByStatus(StatusPedido.ENTREGUE));
        estatisticas.put("pedidosCancelados", pedidoRepository.countByStatus(StatusPedido.CANCELADO));
        
        return estatisticas;
    }

    // Gerar código único do pedido
    private String gerarCodigoPedido() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder codigo = new StringBuilder();
        
        for (int i = 0; i < 9; i++) {
            codigo.append(chars.charAt(random.nextInt(chars.length())));
            if ((i + 1) % 3 == 0 && i < 8) codigo.append("-");
        }
        
        // Verificar se o código já existe
        Optional<Pedido> pedidoExistente = pedidoRepository.findByCodigo(codigo.toString());
        if (pedidoExistente.isPresent()) {
            return gerarCodigoPedido(); // Recursão se o código já existir
        }
        
        return codigo.toString();
    }

    // Classes para requests
    public static class PedidoRequest {
        private String clienteNome;
        private String metodoPagamento;
        private List<ItemPedidoRequest> itens;

        // Getters e Setters
        public String getClienteNome() { return clienteNome; }
        public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
        public String getMetodoPagamento() { return metodoPagamento; }
        public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
        public List<ItemPedidoRequest> getItens() { return itens; }
        public void setItens(List<ItemPedidoRequest> itens) { this.itens = itens; }
    }

    public static class ItemPedidoRequest {
        private Long produtoId;
        private Integer quantidade;

        // Getters e Setters
        public Long getProdutoId() { return produtoId; }
        public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }
        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    }

    public static class StatusRequest {
        private StatusPedido status;

        // Getters e Setters
        public StatusPedido getStatus() { return status; }
        public void setStatus(StatusPedido status) { this.status = status; }
    }

    @GetMapping("/ativos")
public ResponseEntity<?> listarPedidosAtivos() {
    try {
        // ✅ BUSCAR APENAS PEDIDOS NÃO ENTREGUES
        List<Pedido> pedidos = pedidoRepository.findPedidosAtivos();
        
        List<Map<String, Object>> pedidosResponse = new ArrayList<>();
        
        for (Pedido pedido : pedidos) {
            Map<String, Object> pedidoMap = new HashMap<>();
            pedidoMap.put("id", pedido.getId());
            pedidoMap.put("codigo", pedido.getCodigo());
            pedidoMap.put("clienteNome", pedido.getClienteNome());
            pedidoMap.put("total", pedido.getTotal());
            pedidoMap.put("status", pedido.getStatus().toString());
            pedidoMap.put("metodoPagamento", pedido.getMetodoPagamento());
            pedidoMap.put("dataPedido", pedido.getDataPedido());
            pedidoMap.put("dataAtualizacao", pedido.getDataAtualizacao());
            
            // Carregar itens do pedido
            List<PedidoItem> itens = pedidoItemRepository.findByPedidoId(pedido.getId());
            List<Map<String, Object>> itensMap = new ArrayList<>();
            
            for (PedidoItem item : itens) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("quantidade", item.getQuantidade());
                itemMap.put("precoUnitario", item.getPrecoUnitario());
                
                if (item.getProduto() != null) {
                    Map<String, Object> produtoMap = new HashMap<>();
                    produtoMap.put("id", item.getProduto().getId());
                    produtoMap.put("nome", item.getProduto().getNome());
                    produtoMap.put("preco", item.getProduto().getPreco());
                    produtoMap.put("descricao", item.getProduto().getDescricao());
                    itemMap.put("produto", produtoMap);
                }
                
                itensMap.add(itemMap);
            }
            
            pedidoMap.put("itens", itensMap);
            pedidosResponse.add(pedidoMap);
        }
        
        return ResponseEntity.ok(pedidosResponse);
        
    } catch (Exception e) {
        e.printStackTrace();
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Erro ao carregar pedidos ativos: " + e.getMessage());
        return ResponseEntity.status(500).body(errorResponse);
    }
}
}


