package com.ifsp.projetoCantina.repository;

import com.ifsp.projetoCantina.model.TransacaoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransacaoPagamentoRepository extends JpaRepository<TransacaoPagamento, Long> {
    Optional<TransacaoPagamento> findByPedidoId(Long pedidoId);
    Optional<TransacaoPagamento> findByTransacaoId(String transacaoId);
}