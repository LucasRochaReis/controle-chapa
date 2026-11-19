package br.com.techfiles.controlepapel.service;

import br.com.techfiles.controlepapel.model.LookupCategory;
import br.com.techfiles.controlepapel.model.LookupOption;
import br.com.techfiles.controlepapel.repo.AppUserRepository;
import br.com.techfiles.controlepapel.repo.LookupOptionRepository;
import br.com.techfiles.controlepapel.repo.PlateDefectEntryRepository;
import br.com.techfiles.controlepapel.repo.PlateProductionEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class LookupMergeService {

    private final LookupOptionRepository lookupOptionRepository;
    private final AppUserRepository appUserRepository;
    private final PlateProductionEntryRepository productionEntryRepository;
    private final PlateDefectEntryRepository defectEntryRepository;

    public LookupMergeService(
            LookupOptionRepository lookupOptionRepository,
            AppUserRepository appUserRepository,
            PlateProductionEntryRepository productionEntryRepository,
            PlateDefectEntryRepository defectEntryRepository
    ) {
        this.lookupOptionRepository = lookupOptionRepository;
        this.appUserRepository = appUserRepository;
        this.productionEntryRepository = productionEntryRepository;
        this.defectEntryRepository = defectEntryRepository;
    }

    @Transactional
    public MergeResult merge(Long sourceId, Long targetId) {
        if (sourceId == null || targetId == null) {
            throw new IllegalArgumentException("Informe os dois cadastros para unir.");
        }
        if (Objects.equals(sourceId, targetId)) {
            throw new IllegalArgumentException("Os dois cadastros devem ser diferentes.");
        }

        LookupOption source = lookupOptionRepository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Cadastro de origem nao encontrado."));
        LookupOption target = lookupOptionRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Cadastro de destino nao encontrado."));

        if (source.getCategory() != target.getCategory()) {
            throw new IllegalArgumentException("Os dois cadastros devem ser da mesma categoria.");
        }

        LookupCategory category = source.getCategory();
        MergeResult result = new MergeResult();
        result.sourceLabel = source.getLabel();
        result.targetLabel = target.getLabel();
        result.category = category;

        int upd;
        switch (category) {
            case MACHINE:
                upd = productionEntryRepository.remapMachine(sourceId, targetId);
                result.productionRemapped += upd;
                upd = defectEntryRepository.remapMachine(sourceId, targetId);
                result.defectRemapped += upd;
                upd = appUserRepository.remapMachine(sourceId, targetId);
                result.userRemapped += upd;
                break;
            case SHIFT:
                upd = productionEntryRepository.remapShift(sourceId, targetId);
                result.productionRemapped += upd;
                upd = defectEntryRepository.remapShift(sourceId, targetId);
                result.defectRemapped += upd;
                break;
            default:
                break;
        }

        lookupOptionRepository.delete(source);
        lookupOptionRepository.flush();
        return result;
    }

    public static class MergeResult {
        public LookupCategory category;
        public String sourceLabel;
        public String targetLabel;
        public int productionRemapped;
        public int defectRemapped;
        public int userRemapped;

        public Map<String, Integer> summary() {
            Map<String, Integer> m = new HashMap<>();
            if (productionRemapped > 0) m.put("Lancamentos", productionRemapped);
            if (defectRemapped > 0) m.put("Ocorrencias", defectRemapped);
            if (userRemapped > 0) m.put("Usuarios", userRemapped);
            return m;
        }

        public int total() {
            return productionRemapped + defectRemapped + userRemapped;
        }
    }
}
