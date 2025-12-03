package com.ifsp.projetoCantina.repository;

import com.ifsp.projetoCantina.model.Carrinho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CarrinhoRepository extends JpaRepository<Carrinho, Long> {
    List<Carrinho> findBySessaoId(String sessaoId);
    
    Optional<Carrinho> findBySessaoIdAndProdutoId(String sessaoId, Long produtoId);
    
    @Modifying
    @Query("DELETE FROM Carrinho c WHERE c.sessaoId = :sessaoId")
    void limparCarrinho(String sessaoId);
    
    @Modifying
    @Query("DELETE FROM Carrinho c WHERE c.sessaoId = :sessaoId AND c.produto.id = :produtoId")
    void removerItem(String sessaoId, Long produtoId);
}