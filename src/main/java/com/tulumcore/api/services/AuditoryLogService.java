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

    @Transactional
    public AuditoryLog registrar(String accion, String entidad, Long entidadId,
                                  String descripcion, String detalleAnterior, String detalleNuevo) {
        Usuario usuario = getCurrentUser();
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

    public List<AuditoryLog> listar() {
        return repository.findAllByTenantIdOrderByFechaDesc(TenantContext.getCurrentTenant());
    }

    public List<AuditoryLog> buscarPorFiltros(String entidad, String accion,
                                               LocalDateTime desde, LocalDateTime hasta) {
        return repository.buscarPorFiltros(
                TenantContext.getCurrentTenant(), entidad, accion, desde, hasta);
    }
}
