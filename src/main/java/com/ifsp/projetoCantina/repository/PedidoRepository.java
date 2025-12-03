package com.ifsp.projetoCantina.repository;

import com.ifsp.projetoCantina.model.Pedido;
import com.ifsp.projetoCantina.model.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    
    Optional<Pedido> findByCodigo(String codigo);
    
    List<Pedido> findByStatusOrderByDataPedidoAsc(StatusPedido status);
    
    List<Pedido> findAllByOrderByDataPedidoDesc();
    
    List<Pedido> findByClienteNomeContainingIgnoreCase(String clienteNome);

    List<Pedido> findByStatus(StatusPedido status);

    @Query("SELECT p FROM Pedido p WHERE p.status != 'ENTREGUE' ORDER BY " +
    "CASE WHEN p.status = 'PENDENTE' THEN 1 " +
    "     WHEN p.status = 'PREPARANDO' THEN 2 " +
    "     WHEN p.status = 'PRONTO' THEN 3 " +
    "     ELSE 4 END, p.dataPedido DESC")
    List<Pedido> findPedidosAtivos();
    
    // ✅ Deletar por status
    void deleteByStatus(StatusPedido status);
    
    @Query("SELECT p FROM Pedido p WHERE DATE(p.dataPedido) = CURRENT_DATE ORDER BY p.dataPedido DESC")
    List<Pedido> findTodayOrders();

    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.itens i LEFT JOIN FETCH i.produto ORDER BY p.dataPedido DESC")
    List<Pedido> findAllWithItens();
    
    long countByStatus(StatusPedido status);
}