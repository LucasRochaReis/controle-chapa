package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.service.ObservacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/observacoes")
public class ObservacaoApiController {

    private final ObservacaoService observacaoService;

    public ObservacaoApiController(ObservacaoService observacaoService) {
        this.observacaoService = observacaoService;
    }

    /** Retorna sugestoes de observacoes ja usadas, filtradas pelo prefixo informado. */
    @GetMapping
    public ResponseEntity<List<String>> sugerir(@RequestParam(name = "term", required = false) String term) {
        return ResponseEntity.ok(observacaoService.sugerir(term));
    }
}
