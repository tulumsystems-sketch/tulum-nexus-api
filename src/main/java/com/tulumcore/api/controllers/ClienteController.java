package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Cliente;
import com.tulumcore.api.services.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    /**
     * Lista todos los clientes pertenecientes al Tenant actual.
     */
    @GetMapping
    public List<Cliente> getAllClientes() {
        return clienteService.getAllClientes();
    }

    /**
     * Obtiene un cliente específico por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> getClienteById(@PathVariable Long id) {
        return clienteService.getClienteById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Crea un nuevo cliente mapeando los datos desde el DTO.
     */
    @PostMapping
    public ResponseEntity<Cliente> createCliente(@RequestBody ClienteDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setApellido(dto.getApellido());
        cliente.setEmpresa(dto.getEmpresa());

        Cliente creado = clienteService.createOrUpdateCliente(cliente);
        return ResponseEntity.ok(creado);
    }

    /**
     * Actualiza un cliente existente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> updateCliente(@PathVariable Long id, @RequestBody ClienteDTO dto) {
        return clienteService.getClienteById(id)
                .map(existingCliente -> {
                    existingCliente.setNombre(dto.getNombre());
                    existingCliente.setApellido(dto.getApellido());
                    existingCliente.setEmpresa(dto.getEmpresa());

                    Cliente actualizado = clienteService.createOrUpdateCliente(existingCliente);
                    return ResponseEntity.ok(actualizado);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Elimina un cliente por su ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCliente(@PathVariable Long id) {
        if (clienteService.getClienteById(id).isPresent()) {
            clienteService.deleteCliente(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}