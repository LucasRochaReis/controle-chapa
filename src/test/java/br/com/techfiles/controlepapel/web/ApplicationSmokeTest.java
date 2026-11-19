package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.AppUser;
import br.com.techfiles.controlepapel.model.AppUserRole;
import br.com.techfiles.controlepapel.repo.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedTestUsers() {
        seedUser("gestor", AppUserRole.GESTOR);
        seedUser("usuario", AppUserRole.USUARIO);
    }

    private void seedUser(String username, AppUserRole role) {
        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("test"));
        user.setName(username);
        user.setEnabled(true);
        user.setRole(role);
        user.setMustChangePassword(false);
        appUserRepository.save(user);
    }

    @Test
    void loginPageShouldOpen() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedPageShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/lancamentos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "gestor", roles = {"GESTOR"})
    void gestorShouldNotAccessAdminPages() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminShouldAccessAdminPages() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "gestor", roles = {"GESTOR"})
    void gestorShouldAccessReportsPage() throws Exception {
        mockMvc.perform(get("/relatorios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "usuario", roles = {"USUARIO"})
    void usuarioShouldAccessReportsPage() throws Exception {
        mockMvc.perform(get("/relatorios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "gestor", roles = {"GESTOR"})
    void gestorShouldExportReportsExcel() throws Exception {
        mockMvc.perform(get("/relatorios/excel"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=relatorio-controle-chapa.xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }
}
