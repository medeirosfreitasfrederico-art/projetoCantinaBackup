package com.ifsp.projetoCantina.repository;

import com.ifsp.projetoCantina.model.VendedorPergunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VendedorPerguntaRepository extends JpaRepository<VendedorPergunta, Long> {
    
    // Encontrar todas as perguntas de um vendedor
    List<VendedorPergunta> findByVendedorId(Long vendedorId);
    
    // Encontrar uma pergunta específica de um vendedor
    Optional<VendedorPergunta> findByVendedorIdAndPerguntaId(Long vendedorId, Integer perguntaId);
    
    // Verificar se uma resposta está correta
    @Query("SELECT vp FROM VendedorPergunta vp WHERE vp.vendedor.id = :vendedorId AND vp.pergunta.id = :perguntaId AND vp.resposta = :resposta")
    Optional<VendedorPergunta> verificarResposta(
        @Param("vendedorId") Long vendedorId, 
        @Param("perguntaId") Integer perguntaId, 
        @Param("resposta") String resposta
    );
    
    // Contar quantas perguntas um vendedor tem
    long countByVendedorId(Long vendedorId);
}