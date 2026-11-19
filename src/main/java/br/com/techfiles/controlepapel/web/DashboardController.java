package br.com.techfiles.controlepapel.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class DashboardController {

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/lancamentos", true);
    }

    @GetMapping("/dashboard")
    public RedirectView dashboardAntigo() {
        return new RedirectView("/relatorios/dashboard", true);
    }
}
