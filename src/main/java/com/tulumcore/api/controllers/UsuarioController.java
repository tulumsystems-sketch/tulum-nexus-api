package com.tulumcore.api.controllers;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.Rol;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.exceptions.BusinessException;
import com.tulumcore.api.exceptions.ResourceNotFoundException;
import com.tulumcore.api.repositories.UsuarioRepository;
import com.tulumcore.api.services.PedidoSalida;
import com.tulumcore.api.services.TelefonoWhatsApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public List<UsuarioResponseDTO> getAllUsuarios() {
        String tenant = TenantContext.getCurrentTenant();
        return usuarioRepository.findAllByTenantId(tenant)
                .stream()
                .map(this::aDto)
                .toList();
    }

    @GetMapping("/repartidores")
    public List<UsuarioResponseDTO> getRepartidores() {
        String tenant = TenantContext.getCurrentTenant();
        return usuarioRepository.findAllByTenantId(tenant)
                .stream()
                .filter(u -> u.getRol() == Rol.REPARTIDOR)
                .map(this::aDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> createUsuario(@RequestBody UsuarioCreateDTO dto) {
        String tenant = TenantContext.getCurrentTenant();
        if (dto == null) {
            throw new BusinessException("Datos de usuario inválidos.");
        }

        Rol rol = rolAsignableEnTenant(dto.rol());
        String email = texto(dto.email());
        String telefono = TelefonoWhatsApp.normalizar(dto.telefono());

        if (rol == Rol.REPARTIDOR) {
            if (telefono == null && email == null) {
                throw new BusinessException("El cadete necesita WhatsApp o un email de login para /salida.");
            }
            if (telefono != null) {
                asegurarTelefonoLibre(tenant, telefono, null);
            }
            if (email == null) {
                email = "cadete." + telefono + "@" + tenant + ".tulum.local";
            }
        } else {
            if (email == null) {
                throw new BusinessException("El email es obligatorio.");
            }
            if (telefono != null) {
                asegurarTelefonoLibre(tenant, telefono, null);
            }
        }

        if (usuarioRepository.findByEmailAndTenantId(email, tenant).isPresent()) {
            throw new BusinessException("Ya existe un usuario con ese email.");
        }

        String passwordPlano = dto.password();
        if (passwordPlano == null || passwordPlano.isBlank()) {
            if (rol != Rol.REPARTIDOR || telefono == null) {
                throw new BusinessException("La contraseña es obligatoria.");
            }
            passwordPlano = UUID.randomUUID().toString();
        } else if (passwordPlano.length() < 6) {
            throw new BusinessException("La contraseña debe tener al menos 6 caracteres.");
        }

        Usuario nuevo = new Usuario();
        nuevo.setEmail(email);
        nuevo.setPassword(passwordEncoder.encode(passwordPlano));
        nuevo.setRol(rol);
        nuevo.setTelefono(telefono);
        nuevo.setTenantId(tenant);

        Usuario guardado = usuarioRepository.save(nuevo);
        return ResponseEntity.ok(aDto(guardado));
    }

    @PutMapping("/{id}/rol")
    public UsuarioResponseDTO cambiarRol(@PathVariable Long id, @RequestBody RolUpdateDTO dto) {
        String tenant = TenantContext.getCurrentTenant();
        Usuario usuario = usuarioRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        Rol nuevo = rolAsignableEnTenant(dto.rol());
        usuario.setRol(nuevo);
        usuarioRepository.save(usuario);
        return aDto(usuario);
    }

    @PutMapping("/{id}/telefono")
    public UsuarioResponseDTO cambiarTelefono(@PathVariable Long id, @RequestBody TelefonoUpdateDTO dto) {
        String tenant = TenantContext.getCurrentTenant();
        Usuario usuario = usuarioRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        String telefono = TelefonoWhatsApp.normalizar(dto != null ? dto.telefono() : null);
        if (telefono != null) {
            asegurarTelefonoLibre(tenant, telefono, usuario.getId());
        }
        usuario.setTelefono(telefono);
        usuarioRepository.save(usuario);
        return aDto(usuario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUsuario(@PathVariable Long id) {
        String tenant = TenantContext.getCurrentTenant();
        Usuario usuario = usuarioRepository.findByIdAndTenantId(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
        usuarioRepository.delete(usuario);
        return ResponseEntity.noContent().build();
    }

    private UsuarioResponseDTO aDto(Usuario u) {
        return new UsuarioResponseDTO(
                u.getId(),
                u.getEmail(),
                u.getRol().name(),
                u.getTelefono(),
                PedidoSalida.nombreVisible(u)
        );
    }

    private void asegurarTelefonoLibre(String tenant, String telefono, Long excluirId) {
        usuarioRepository.findByTenantIdAndTelefono(tenant, telefono)
                .filter(u -> excluirId == null || !excluirId.equals(u.getId()))
                .ifPresent(u -> {
                    throw new BusinessException("Ese WhatsApp ya está vinculado a otro usuario.");
                });
    }

    private String texto(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private Rol rolAsignableEnTenant(String raw) {
        if (raw == null || raw.isBlank()) {
            return Rol.OPERADOR;
        }
        final Rol rol;
        try {
            rol = Rol.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Rol invalido.");
        }
        if (rol == Rol.SUPER_ADMIN) {
            throw new BusinessException("No se puede asignar SUPER_ADMIN desde el comercio.");
        }
        return rol;
    }
}
