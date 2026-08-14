package com.tulumcore.api.services;

import com.tulumcore.api.config.TenantContext;
import com.tulumcore.api.entities.AuditoryLog;
import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.repositories.AuditoryLogRepository;
import com.tulumcore.api.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoryLogService {

    @Autowired
    private AuditoryLogRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Usuario getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenant = TenantContext.getCurrentTenant();
        return usuarioRepository.findByEmailAndTenantId(email, tenant)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + email));
    }

    public String detalle(Object... campos) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i + 1 < campos.length; i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("\"").append(escape(campos[i])).append("\": ");
            Object valor = campos[i + 1];
            if (valor instanceof Number || valor instanceof Boolean) {
                sb.append(valor);
            } else {
                sb.append("\"").append(escape(valor)).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    @Transactional
    public AuditoryLog registrar(String accion, String entidad, Long entidadId,
                                  String descripcion, String detalleAnterior, String detalleNuevo) {
        Usuario usuario = null;
        try {
            usuario = getCurrentUser();
        } catch (RuntimeException ignored) {
            // Algunos flujos externos usan una identidad tecnica sin Usuario persistido.
        }
        AuditoryLog log = new AuditoryLog();
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setEntidadId(entidadId);
        log.setDescripcion(descripcion);
        log.setDetalleAnterior(detalleAnterior);
        log.setDetalleNuevo(detalleNuevo);
        log.setUsuario(usuario);
        log.setFecha(LocalDateTime.now());
        log.setTenantId(TenantContext.getCurrentTenant());
        return repository.save(log);
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    public List<AuditoryLog> listar() {
        return repository.findAllByTenantIdOrderByFechaDesc(TenantContext.getCurrentTenant());
    }

    public List<AuditoryLog> buscarPorFiltros(String entidad, String accion,
                                               LocalDateTime desde, LocalDateTime hasta) {
        return repository.buscarPorFiltros(
                TenantContext.getCurrentTenant(), entidad, accion, desde, hasta);
    }
}
