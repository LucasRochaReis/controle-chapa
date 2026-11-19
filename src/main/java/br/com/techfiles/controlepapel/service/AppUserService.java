package br.com.techfiles.controlepapel.service;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.model.AppUserRole;
import br.com.techfiles.controlepapel.repo.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser getRequiredByUsername(String username) {
        return appUserRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));
    }

    public AppUser getRequiredCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Usuario autenticado nao encontrado.");
        }
        return getRequiredByUsername(authentication.getName());
    }

    public boolean isAdmin(AppUser user) {
        return user.getRole() == AppUserRole.ADMIN;
    }

    public boolean isGestor(AppUser user) {
        return user.getRole() == AppUserRole.GESTOR;
    }

    public boolean isVisualizador(AppUser user) {
        return user.getRole() == AppUserRole.VISUALIZADOR;
    }

    public boolean isUsuario(AppUser user) {
        return user.getRole() == AppUserRole.USUARIO;
    }

    public boolean isAdminOrGestorOrVisualizador(AppUser user) {
        return isAdmin(user) || isGestor(user) || isVisualizador(user);
    }

    public boolean hasRelatoriosAccess(AppUser user) {
        return isAdmin(user) || isGestor(user) || isVisualizador(user) || isUsuario(user);
    }
}
