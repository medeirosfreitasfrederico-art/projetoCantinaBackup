package com.ifsp.projetoCantina.repository;

import com.ifsp.projetoCantina.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    
    List<Produto> findByAtivoTrue();
    
    List<Produto> findByAtivoTrueOrderByNome();
    
    Optional<Produto> findByNome(String nome);
    
    List<Produto> findByNomeContainingIgnoreCaseAndAtivoTrue(String nome);
    
    @Query("SELECT p FROM Produto p WHERE p.estoque > 0 AND p.ativo = true ORDER BY p.nome")
    List<Produto> findAvailableProducts();
    
    List<Produto> findByVendedorId(Long vendedorId);
}