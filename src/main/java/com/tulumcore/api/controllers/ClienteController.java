package com.tulumcore.api.controllers;

import com.tulumcore.api.entities.Cliente;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
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

    @GetMapping
    public List<ClienteResponseDTO> getAllClientes() {
        return clienteService.getAllClientes()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public ClienteResponseDTO getClienteById(@PathVariable Long id) {
        return clienteService.getClienteById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> createCliente(@RequestBody ClienteDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setApellido(dto.getApellido());
        cliente.setEmpresa(dto.getEmpresa());

        Cliente creado = clienteService.createOrUpdateCliente(cliente);
        return ResponseEntity.ok(toDTO(creado));
    }

    @PutMapping("/{id}")
    public ClienteResponseDTO updateCliente(@PathVariable Long id, @RequestBody ClienteDTO dto) {
        Cliente existente = clienteService.getClienteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        existente.setNombre(dto.getNombre());
        existente.setApellido(dto.getApellido());
        existente.setEmpresa(dto.getEmpresa());

        return toDTO(clienteService.createOrUpdateCliente(existente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCliente(@PathVariable Long id) {
        clienteService.getClienteById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
        clienteService.deleteCliente(id);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // Mapper privado: entidad → DTO
    // =============================================
    private ClienteResponseDTO toDTO(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getApellido(),
                cliente.getEmpresa()
        );
    }
}