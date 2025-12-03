package com.ifsp.projetoCantina.repository;

import com.ifsp.projetoCantina.model.PedidoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {
    
    List<PedidoItem> findByPedidoId(Long pedidoId);
    
    List<PedidoItem> findByProdutoId(Long produtoId);

    void deleteByPedidoId(Long pedidoId);
}