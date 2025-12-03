package com.ifsp.projetoCantina.controller;

import com.ifsp.projetoCantina.config.MercadoPagoConfig;
import com.ifsp.projetoCantina.model.Pedido;
import com.ifsp.projetoCantina.model.TransacaoPagamento;
import com.ifsp.projetoCantina.repository.PedidoRepository;
import com.ifsp.projetoCantina.repository.TransacaoPagamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagamentos")
@CrossOrigin(origins = "*")
public class PagamentoController {

    @Autowired
    private MercadoPagoConfig mercadoPagoConfig;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private TransacaoPagamentoRepository transacaoPagamentoRepository;

    // Criar pagamento PIX
    @PostMapping("/pix/criar")
    public ResponseEntity<?> criarPagamentoPix(@RequestBody PagamentoRequest request) {
        try {
            Optional<Pedido> pedidoOpt = pedidoRepository.findById(request.getPedidoId());
            
            if (!pedidoOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Pedido não encontrado"));
            }

            Pedido pedido = pedidoOpt.get();
            
            // Criar transação no Mercado Pago
            Map<String, Object> response = criarTransacaoMercadoPago(pedido);
            
            if (response.get("success").equals(true)) {
                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("qr_code", response.get("qr_code"));
                successResponse.put("qr_code_text", response.get("qr_code_text"));
                successResponse.put("transacao_id", response.get("transacao_id"));
                successResponse.put("expiracao", response.get("expiracao"));
                return ResponseEntity.ok(successResponse);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false, 
                "message", "Erro ao criar pagamento: " + e.getMessage()
            ));
        }
    }

    // Verificar status do pagamento
    @GetMapping("/status/{pedidoId}")
    public ResponseEntity<?> verificarStatusPagamento(@PathVariable Long pedidoId) {
        try {
            Optional<TransacaoPagamento> transacaoOpt = transacaoPagamentoRepository.findByPedidoId(pedidoId);
            
            if (!transacaoOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Transação não encontrada"));
            }

            TransacaoPagamento transacao = transacaoOpt.get();
            
            // Verificar status no Mercado Pago
            Map<String, Object> status = verificarStatusMercadoPago(transacao.getTransacaoId());
            
            return ResponseEntity.ok(status);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false, 
                "message", "Erro ao verificar status: " + e.getMessage()
            ));
        }
    }

    // Método para atualizar status do pagamento (usado pelo webhook)
private void atualizarStatusPagamento(String transacaoId) {
    try {
        // Buscar transação no banco
        Optional<TransacaoPagamento> transacaoOpt = transacaoPagamentoRepository.findByTransacaoId(transacaoId);
        
        if (transacaoOpt.isPresent()) {
            TransacaoPagamento transacao = transacaoOpt.get();
            
            // Buscar status atual no Mercado Pago
            Map<String, Object> statusResponse = verificarStatusMercadoPago(transacaoId);
            
            if (statusResponse.get("success").equals(true)) {
                String statusMercadoPago = (String) statusResponse.get("status");
                Pedido pedido = transacao.getPedido();
                
                // Atualizar status local baseado no status do Mercado Pago
                if ("approved".equals(statusMercadoPago)) {
                    transacao.setStatus(TransacaoPagamento.Status.APROVADO);
                    transacao.setDataAprovacao(LocalDateTime.now());
                    pedido.setStatusPagamento(Pedido.StatusPagamento.APROVADO);
                    
                    // Aqui você pode adicionar outras ações quando o pagamento é aprovado
                    // como enviar email, notificação, etc.
                    
                } else if ("rejected".equals(statusMercadoPago)) {
                    transacao.setStatus(TransacaoPagamento.Status.RECUSADO);
                    pedido.setStatusPagamento(Pedido.StatusPagamento.CANCELADO);
                    
                } else if ("cancelled".equals(statusMercadoPago)) {
                    transacao.setStatus(TransacaoPagamento.Status.EXPIRADO);
                    pedido.setStatusPagamento(Pedido.StatusPagamento.CANCELADO);
                    
                } else if ("in_process".equals(statusMercadoPago)) {
                    // Pagamento ainda em processamento - mantém como pendente
                    transacao.setStatus(TransacaoPagamento.Status.PENDENTE);
                    pedido.setStatusPagamento(Pedido.StatusPagamento.PENDENTE);
                }
                
                // Salvar alterações
                transacaoPagamentoRepository.save(transacao);
                pedidoRepository.save(pedido);
                
                System.out.println("✅ Status atualizado - Transação: " + transacaoId + ", Status: " + statusMercadoPago);
            }
        }
    } catch (Exception e) {
        System.err.println("❌ Erro ao atualizar status do pagamento: " + e.getMessage());
    }
}

// Webhook para receber notificações do Mercado Pago
@PostMapping("/webhook/mercadopago")
public ResponseEntity<?> webhookMercadoPago(@RequestBody Map<String, Object> notification) {
    try {
        System.out.println("📨 Webhook recebido: " + notification);
        
        String action = (String) notification.get("action");
        Map<String, Object> data = (Map<String, Object>) notification.get("data");
        
        if (data == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Dados não encontrados"));
        }
        
        String transacaoId = (String) data.get("id");
        
        if (transacaoId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "ID da transação não encontrado"));
        }
        
        if ("payment.updated".equals(action)) {
            // ✅ AGORA CHAMA O MÉTODO CORRETO
            atualizarStatusPagamento(transacaoId);
        }
        
        return ResponseEntity.ok().build();
        
    } catch (Exception e) {
        System.err.println("❌ Erro no webhook: " + e.getMessage());
        return ResponseEntity.status(500).build();
    }
}

    // ========== MÉTODOS PRIVADOS ==========

    private Map<String, Object> criarTransacaoMercadoPago(Pedido pedido) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            // Headers com autenticação
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + mercadoPagoConfig.getAccessToken());
            
            // Corpo da requisição
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transaction_amount", pedido.getTotal().floatValue());
            requestBody.put("description", "Pedido #" + pedido.getCodigo());
            requestBody.put("payment_method_id", "pix");
            requestBody.put("payer", Map.of(
                "email", "cliente@cantina.com", // Em produção, usar email real
                "first_name", pedido.getClienteNome().split(" ")[0]
            ));
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            String url = mercadoPagoConfig.getBaseUrl() + "/v1/payments";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            Map<String, Object> responseBody = response.getBody();
            
            if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                // Salvar transação no banco
                TransacaoPagamento transacao = new TransacaoPagamento();
                transacao.setPedido(pedido);
                transacao.setTransacaoId((String) responseBody.get("id"));
                transacao.setQrCode((String) ((Map) responseBody.get("point_of_interaction")).get("qr_code"));
                transacao.setQrCodeText((String) ((Map) ((Map) responseBody.get("point_of_interaction")).get("transaction_data")).get("qr_code"));
                transacao.setValor(pedido.getTotal());
                transacao.setDataExpiracao(LocalDateTime.now().plusMinutes(30)); // PIX expira em 30min
                
                transacaoPagamentoRepository.save(transacao);
                
                // Atualizar status do pedido
                pedido.setStatusPagamento(Pedido.StatusPagamento.PENDENTE);
                pedidoRepository.save(pedido);
                
                return Map.of(
                    "success", true,
                    "qr_code", transacao.getQrCode(),
                    "qr_code_text", transacao.getQrCodeText(),
                    "transacao_id", transacao.getTransacaoId(),
                    "expiracao", transacao.getDataExpiracao()
                );
            } else {
                return Map.of("success", false, "message", "Erro ao criar pagamento no gateway");
            }
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Erro: " + e.getMessage());
        }
    }

    private Map<String, Object> verificarStatusMercadoPago(String transacaoId) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + mercadoPagoConfig.getAccessToken());
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            String url = mercadoPagoConfig.getBaseUrl() + "/v1/payments/" + transacaoId;
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            
            Map<String, Object> responseBody = response.getBody();
            String status = (String) responseBody.get("status");
            
            // Atualizar status local
            atualizarStatusLocal(transacaoId, status);
            
            return Map.of(
                "success", true,
                "status", status,
                "aprovado", "approved".equals(status)
            );
            
        } catch (Exception e) {
            return Map.of("success", false, "message", "Erro ao verificar status");
        }
    }

    private void atualizarStatusLocal(String transacaoId, String statusMercadoPago) {
        Optional<TransacaoPagamento> transacaoOpt = transacaoPagamentoRepository.findByTransacaoId(transacaoId);
        
        if (transacaoOpt.isPresent()) {
            TransacaoPagamento transacao = transacaoOpt.get();
            Pedido pedido = transacao.getPedido();
            
            if ("approved".equals(statusMercadoPago)) {
                transacao.setStatus(TransacaoPagamento.Status.APROVADO);
                transacao.setDataAprovacao(LocalDateTime.now());
                pedido.setStatusPagamento(Pedido.StatusPagamento.APROVADO);
            } else if ("rejected".equals(statusMercadoPago)) {
                transacao.setStatus(TransacaoPagamento.Status.RECUSADO);
                pedido.setStatusPagamento(Pedido.StatusPagamento.CANCELADO);
            } else if ("cancelled".equals(statusMercadoPago)) {
                transacao.setStatus(TransacaoPagamento.Status.EXPIRADO);
                pedido.setStatusPagamento(Pedido.StatusPagamento.CANCELADO);
            }
            
            transacaoPagamentoRepository.save(transacao);
            pedidoRepository.save(pedido);
        }
    }

    // Classes Request
    public static class PagamentoRequest {
        private Long pedidoId;
        
        public Long getPedidoId() { return pedidoId; }
        public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }
    }
}