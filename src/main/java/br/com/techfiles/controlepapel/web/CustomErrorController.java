package br.com.techfiles.controlepapel.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;

@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object messageObj = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exceptionObj = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        int status = 500;
        if (statusObj != null) {
            try {
                status = Integer.parseInt(statusObj.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        HttpStatus httpStatus = HttpStatus.resolve(status);
        String message = null;
        if (messageObj != null && !messageObj.toString().isBlank()) {
            message = messageObj.toString();
        } else if (httpStatus != null && !httpStatus.getReasonPhrase().isBlank()) {
            message = httpStatus.getReasonPhrase();
        }

        if (message == null || message.isBlank()) {
            if (status == 403) message = "Voce nao tem permissao para acessar este recurso.";
            else if (status == 404) message = "A pagina solicitada nao existe.";
            else if (status == 500) message = "Ocorreu um erro interno no processamento da requisicao.";
        }

        if (exceptionObj instanceof Throwable th && th.getMessage() != null && !th.getMessage().isBlank() && (status == 500 || (message != null && message.length() < 20))) {
            message = th.getMessage();
        }

        model.addAttribute("status", status);
        model.addAttribute("message", message);
        model.addAttribute("timestamp", LocalDateTime.now());

        if (status == HttpStatus.FORBIDDEN.value() && request.getUserPrincipal() == null) {
            return "redirect:/login";
        }
        return "error";
    }
}
