package br.com.techfiles.controlepapel.service;

import br.com.techfiles.controlepapel.repo.PlateDefectEntryRepository;
import br.com.techfiles.controlepapel.repo.PlateProductionEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sugere textos de observacao ja utilizados em lancamentos de producao e ocorrencias.
 * E apenas uma sugestao: o campo continua livre para digitar qualquer texto.
 */
@Service
public class ObservacaoService {

    private static final int LIMITE_SUGESTOES = 30;

    private final PlateProductionEntryRepository plateProductionEntryRepository;
    private final PlateDefectEntryRepository plateDefectEntryRepository;

    public ObservacaoService(PlateProductionEntryRepository plateProductionEntryRepository,
                             PlateDefectEntryRepository plateDefectEntryRepository) {
        this.plateProductionEntryRepository = plateProductionEntryRepository;
        this.plateDefectEntryRepository = plateDefectEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<String> sugerir(String term) {
        String prefixo = term == null ? "" : term.trim();
        List<String> todas = new ArrayList<>();
        todas.addAll(plateProductionEntryRepository.findObservacoesByPrefixo(prefixo));
        todas.addAll(plateDefectEntryRepository.findObservacoesByPrefixo(prefixo));
        return todas.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(texto -> !texto.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(LIMITE_SUGESTOES)
                .toList();
    }
}
