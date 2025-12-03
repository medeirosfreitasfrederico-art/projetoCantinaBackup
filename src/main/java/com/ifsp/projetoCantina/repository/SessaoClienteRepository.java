package com.ifsp.projetoCantina.repository;

import com.ifsp.projetoCantina.model.SessaoCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SessaoClienteRepository extends JpaRepository<SessaoCliente, String> {
    Optional<SessaoCliente> findByIdAndAtivaTrue(String id);
    
    @Modifying
    @Query("UPDATE SessaoCliente s SET s.dataUltimaAtividade = :agora WHERE s.id = :sessaoId")
    void atualizarAtividade(String sessaoId, LocalDateTime agora);
    
    @Modifying
    @Query("UPDATE SessaoCliente s SET s.ativa = false WHERE s.dataUltimaAtividade < :limite")
    void desativarSessoesExpiradas(LocalDateTime limite);
}