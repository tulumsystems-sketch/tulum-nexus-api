package com.tulumcore.api.controllers;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Rol;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Solo lista usuarios del tenant actual
    @GetMapping
    public List<UsuarioResponseDTO> getAllUsuarios() {
        String tenant = TenantContext.getCurrentTenant();
        return usuarioRepository.findAllByTenantId(tenant)
                .stream()
                .map(u -> new UsuarioResponseDTO(u.getId(), u.getEmail(), u.getRol().name()))
                .toList();
    }

    // Crear nuevo usuario dentro del tenant
    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> createUsuario(@RequestBody UsuarioCreateDTO dto) {
        String tenant = TenantContext.getCurrentTenant();

        if (usuarioRepository.findByEmailAndTenantId(dto.email(), tenant).isPresent()) {
            throw new BusinessException("Ya existe un usuario con ese email.");
        }

        Usuario nuevo = new Usuario();
        nuevo.setEmail(dto.email());
        nuevo.setPassword(passwordEncoder.encode(dto.password()));
        nuevo.setRol(dto.rol() != null ? Rol.valueOf(dto.rol()) : Rol.OPERADOR);
        nuevo.setTenantId(tenant);

        Usuario guardado = usuarioRepository.save(nuevo);
        return ResponseEntity.ok(new UsuarioResponseDTO(guardado.getId(), guardado.getEmail(), guardado.getRol().name()));
    }

    // Cambiar rol de un usuario
    @PutMapping("/{id}/rol")
    public UsuarioResponseDTO cambiarRol(@PathVariable Long id, @RequestBody RolUpdateDTO dto) {
        String tenant = TenantContext.getCurrentTenant();

        Usuario usuario = usuarioRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        usuario.setRol(Rol.valueOf(dto.rol()));
        usuarioRepository.save(usuario);

        return new UsuarioResponseDTO(usuario.getId(), usuario.getEmail(), usuario.getRol().name());
    }

    // Eliminar usuario del tenant
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUsuario(@PathVariable Long id) {
        String tenant = TenantContext.getCurrentTenant();

        Usuario usuario = usuarioRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        usuarioRepository.delete(usuario);
        return ResponseEntity.noContent().build();
    }
}
