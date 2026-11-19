package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.repo.AppUserRepository;
import br.com.techfiles.controlepapel.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FirstAccessPasswordController {

    private final AppUserRepository appUserRepository;
    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;

    public FirstAccessPasswordController(
            AppUserRepository appUserRepository,
            AppUserService appUserService,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.appUserService = appUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/trocar-senha-primeiro-acesso")
    public String showChangePasswordForm(Model model, Authentication authentication) {
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new FirstAccessPasswordForm());
        }
        AppUser user = appUserService.getRequiredCurrentUser(authentication);
        model.addAttribute("pageTitle", "Trocar senha no primeiro acesso");
        model.addAttribute("userName", user.getName());
        model.addAttribute("mustChange", user.isMustChangePassword());
        return "auth/change-password-first-access";
    }

    @PostMapping("/trocar-senha-primeiro-acesso")
    public String processChangePassword(
            @Valid @ModelAttribute("passwordForm") FirstAccessPasswordForm form,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        String newPassword = form.getNewPassword();
        String confirmPassword = form.getConfirmPassword();

        if (newPassword != null && confirmPassword != null && !newPassword.equals(confirmPassword)) {
            bindingResult.rejectValue("confirmPassword", "match", "A confirmação da senha não coincide com a nova senha.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/change-password-first-access";
        }

        AppUser user = appUserService.getRequiredCurrentUser(authentication);
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setMustChangePassword(false);
        appUserRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Senha atualizada com sucesso. Bem-vindo(a) ao sistema!");
        return "redirect:/dashboard";
    }
}
