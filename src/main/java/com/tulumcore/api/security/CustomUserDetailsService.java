package com.tulumcore.api.security;

import com.tulumcore.api.entities.Usuario;
import com.tulumcore.api.repositories.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Buscamos al usuario por el email que llega del front
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        // Transformamos nuestra entidad Usuario al objeto "User" que entiende Spring Security
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword()) // Esta contraseña ya debe venir encriptada en Bcrypt desde la DB
                .authorities("ROLE_USER") // Permisos por defecto
                .build();
    }
}