package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.AppUserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminUserForm {

    @NotBlank(message = "Informe o nome.")
    private String name;

    @NotBlank(message = "Informe o username.")
    private String username;

    @NotBlank(message = "Informe a senha.")
    @Size(min = 4, message = "A senha deve ter pelo menos 4 caracteres.")
    private String password;

    @NotNull(message = "Selecione o perfil.")
    private AppUserRole role = AppUserRole.USUARIO;

    private boolean mustChangePassword = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AppUserRole getRole() {
        return role;
    }

    public void setRole(AppUserRole role) {
        this.role = role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
