package br.com.techfiles.controlepapel.config;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.model.AppUserRole;
import br.com.techfiles.controlepapel.repo.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SeedDataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public SeedDataInitializer(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createDefaultUser();
    }

    private void createDefaultUser() {
        String adminUsername = System.getenv().getOrDefault("APP_ADMIN_USERNAME", "admin");
        String adminPassword = System.getenv().getOrDefault("APP_ADMIN_PASSWORD", "admin123");

        AppUser existingUser = appUserRepository.findByUsernameIgnoreCase(adminUsername).orElse(null);
        if (existingUser != null) {
            if (existingUser.getRole() != AppUserRole.ADMIN) {
                existingUser.setRole(AppUserRole.ADMIN);
                existingUser.setMachine(null);
                appUserRepository.save(existingUser);
            }
            return;
        }

        AppUser user = new AppUser();
        user.setUsername(adminUsername);
        user.setPassword(passwordEncoder.encode(adminPassword));
        user.setName("Administrador");
        user.setEnabled(true);
        user.setRole(AppUserRole.ADMIN);
        user.setMustChangePassword(false);
        appUserRepository.save(user);
    }
}
