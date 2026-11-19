package br.com.techfiles.controlepapel.service;

import br.com.techfiles.controlepapel.model.Cliente;
import br.com.techfiles.controlepapel.repo.ClienteRepository;
import br.com.techfiles.controlepapel.repo.PlateDefectEntryRepository;
import br.com.techfiles.controlepapel.repo.PlateProductionEntryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ClienteService {

    private static final Pattern ACENTOS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final ClienteRepository clienteRepository;
    private final PlateProductionEntryRepository plateProductionEntryRepository;
    private final PlateDefectEntryRepository plateDefectEntryRepository;

    public ClienteService(ClienteRepository clienteRepository,
                          PlateProductionEntryRepository plateProductionEntryRepository,
                          PlateDefectEntryRepository plateDefectEntryRepository) {
        this.clienteRepository = clienteRepository;
        this.plateProductionEntryRepository = plateProductionEntryRepository;
        this.plateDefectEntryRepository = plateDefectEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<Cliente> buscarPorPrefixo(String term) {
        String prefixo = normalizar(term);
        if (prefixo == null || prefixo.isEmpty()) {
            return clienteRepository.findAllByOrderByNomeAsc();
        }
        return clienteRepository.findByPrefixo(prefixo);
    }

    @Transactional
    public String garantirCliente(String nomeBruto) {
        String normalizado = normalizar(nomeBruto);
        if (normalizado == null || normalizado.isEmpty()) {
            return normalizado;
        }
        if (!clienteRepository.existsByNome(normalizado)) {
            Cliente c = new Cliente();
            c.setNome(normalizado);
            clienteRepository.save(c);
        }
        return normalizado;
    }

    @Transactional
    public ExclusaoResultado excluir(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."));

        String nomeCliente = cliente.getNome();
        long lancamentosProducao = plateProductionEntryRepository.countByClientName(nomeCliente);
        long lancamentosDefeito = plateDefectEntryRepository.countByClientName(nomeCliente);
        if (lancamentosProducao > 0 || lancamentosDefeito > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Não foi possível excluir o cliente porque ele está vinculado a ");
            boolean first = true;
            if (lancamentosProducao > 0) {
                sb.append(lancamentosProducao).append(" lançamento(s)");
                first = false;
            }
            if (lancamentosDefeito > 0) {
                if (!first) sb.append(", ");
                sb.append(lancamentosDefeito).append(" lançamento(s) de ocorrência");
            }
            sb.append(".");
            return new ExclusaoResultado(false, sb.toString());
        }

        try {
            clienteRepository.delete(cliente);
            clienteRepository.flush();
            return new ExclusaoResultado(true, "Cliente excluido com sucesso.");
        } catch (DataIntegrityViolationException ex) {
            return new ExclusaoResultado(false, "Nao foi possivel excluir este cliente porque ele ja esta em uso.");
        }
    }

    public String normalizar(String valor) {
        if (valor == null) return null;
        String semEspacos = valor.trim();
        if (semEspacos.isEmpty()) return null;
        String semAcentos = Normalizer.normalize(semEspacos, Normalizer.Form.NFD);
        semAcentos = ACENTOS.matcher(semAcentos).replaceAll("");
        return semAcentos.toUpperCase(Locale.ROOT);
    }

    public record ExclusaoResultado(boolean sucesso, String mensagem) {
    }
}
