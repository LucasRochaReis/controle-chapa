package br.com.techfiles.controlepapel.config;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.service.AppUserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AppUserService appUserService;

    public CustomAuthenticationSuccessHandler(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        AppUser user = appUserService.getRequiredCurrentUser(authentication);
        if (user.isMustChangePassword()) {
            response.sendRedirect(request.getContextPath() + "/trocar-senha-primeiro-acesso");
        } else {
            response.sendRedirect(request.getContextPath() + "/lancamentos");
        }
    }
}
