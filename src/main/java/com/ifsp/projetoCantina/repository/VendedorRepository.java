package com.ifsp.projetoCantina.repository;

import com.ifsp.projetoCantina.model.Vendedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendedorRepository extends JpaRepository<Vendedor, Long> {
    Optional<Vendedor> findByUsuario(String usuario);
    Optional<Vendedor> findByUsuarioAndSenha(String usuario, String senha);
    Optional<Vendedor> findByEmail(String email);
    Optional<Vendedor> findByCpf(String cpf);

    List<Vendedor> findByValidado(Vendedor.Validado validado);
}

