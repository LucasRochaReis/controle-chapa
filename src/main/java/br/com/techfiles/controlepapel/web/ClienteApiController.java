package br.com.techfiles.controlepapel.web;

import br.com.techfiles.controlepapel.model.Cliente;
import br.com.techfiles.controlepapel.service.ClienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteApiController {

    private final ClienteService clienteService;

    public ClienteApiController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public ResponseEntity<List<ClienteItem>> buscar(@RequestParam(name = "term", required = false) String term) {
        List<ClienteItem> itens = clienteService.buscarPorPrefixo(term).stream()
                .map(ClienteApiController::paraItem)
                .toList();
        return ResponseEntity.ok(itens);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ExclusaoResponse> excluir(@PathVariable Long id) {
        ClienteService.ExclusaoResultado resultado = clienteService.excluir(id);
        ExclusaoResponse corpo = new ExclusaoResponse(resultado.sucesso(), resultado.mensagem());
        return resultado.sucesso()
                ? ResponseEntity.ok(corpo)
                : ResponseEntity.badRequest().body(corpo);
    }

    private static ClienteItem paraItem(Cliente cliente) {
        return new ClienteItem(cliente.getId(), cliente.getNome());
    }

    public record ClienteItem(Long id, String nome) {
    }

    public record ExclusaoResponse(boolean sucesso, String mensagem) {
    }
}
