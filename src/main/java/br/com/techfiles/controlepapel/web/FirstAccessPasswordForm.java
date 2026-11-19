package br.com.techfiles.controlepapel.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FirstAccessPasswordForm {

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 4, message = "A nova senha deve ter pelo menos 4 caracteres.")
    private String newPassword;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
