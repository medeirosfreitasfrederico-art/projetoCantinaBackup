package com.ifsp.projetoCantina.repository;

import com.ifsp.projetoCantina.model.PerguntaSeguranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerguntaSegurancaRepository extends JpaRepository<PerguntaSeguranca, Integer> {
    
    // Encontrar todas as perguntas ativas
    List<PerguntaSeguranca> findByAtivaTrue();
    
    // Encontrar pergunta por texto
    Optional<PerguntaSeguranca> findByPergunta(String pergunta);
    
    // Buscar perguntas aleatórias (útil para o cadastro)
    @Query(value = "SELECT * FROM perguntas_seguranca WHERE ativa = true ORDER BY RAND() LIMIT :quantidade", nativeQuery = true)
    List<PerguntaSeguranca> findRandomPerguntas(@Param("quantidade") int quantidade);
}
