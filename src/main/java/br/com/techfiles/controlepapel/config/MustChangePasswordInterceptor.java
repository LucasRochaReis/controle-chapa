package br.com.techfiles.controlepapel.config;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.service.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MustChangePasswordInterceptor implements HandlerInterceptor {

    private static final String CHANGE_PASSWORD_PATH = "/trocar-senha-primeiro-acesso";
    private static final String LOGOUT_PATH = "/logout";

    private final AppUserService appUserService;

    public MustChangePasswordInterceptor(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;

        if (path.startsWith(CHANGE_PASSWORD_PATH) || path.startsWith(LOGOUT_PATH)) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true;
        }

        if ("anonymousUser".equals(authentication.getPrincipal())) {
            return true;
        }

        AppUser user;
        try {
            user = appUserService.getRequiredCurrentUser(authentication);
        } catch (Exception ignored) {
            return true;
        }

        if (user.isMustChangePassword()) {
            response.sendRedirect(request.getContextPath() + CHANGE_PASSWORD_PATH);
            return false;
        }

        return true;
    }
}
