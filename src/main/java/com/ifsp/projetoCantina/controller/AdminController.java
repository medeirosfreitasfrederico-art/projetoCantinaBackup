package com.ifsp.projetoCantina.controller;

import com.ifsp.projetoCantina.model.Vendedor;
import com.ifsp.projetoCantina.repository.VendedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private VendedorRepository vendedorRepository;

    // Listar vendedores não validados
    @GetMapping("/vendedores/pendentes")
    public ResponseEntity<?> listarVendedoresPendentes() {
        try {
            List<Vendedor> vendedoresPendentes = vendedorRepository.findByValidado(Vendedor.Validado.NÃO);
            return ResponseEntity.ok(vendedoresPendentes);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao carregar vendedores pendentes: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Validar vendedor
    @PutMapping("/vendedores/{id}/validar")
    public ResponseEntity<?> validarVendedor(@PathVariable Long id) {
        try {
            Optional<Vendedor> vendedorOpt = vendedorRepository.findById(id);
            
            if (!vendedorOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Vendedor não encontrado");
                return ResponseEntity.status(404).body(response);
            }

            Vendedor vendedor = vendedorOpt.get();
            vendedor.setValidado(Vendedor.Validado.SIM);
            vendedorRepository.save(vendedor);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vendedor validado com sucesso");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao validar vendedor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Rejeitar vendedor (opcional - deletar conta)
    @DeleteMapping("/vendedores/{id}")
    public ResponseEntity<?> rejeitarVendedor(@PathVariable Long id) {
        try {
            Optional<Vendedor> vendedorOpt = vendedorRepository.findById(id);
            
            if (!vendedorOpt.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Vendedor não encontrado");
                return ResponseEntity.status(404).body(response);
            }

            vendedorRepository.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Vendedor rejeitado e removido com sucesso");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao rejeitar vendedor: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Listar todos os vendedores (para admin)
    @GetMapping("/vendedores")
    public ResponseEntity<?> listarTodosVendedores() {
        try {
            List<Vendedor> vendedores = vendedorRepository.findAll();
            return ResponseEntity.ok(vendedores);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao carregar vendedores: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}