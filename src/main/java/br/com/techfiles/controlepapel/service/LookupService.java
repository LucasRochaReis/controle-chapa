package br.com.techfiles.controlepapel.service;

import br.com.techfiles.controlepapel.model.LookupCategory;
import br.com.techfiles.controlepapel.model.LookupOption;
import br.com.techfiles.controlepapel.repo.LookupOptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LookupService {

    private final LookupOptionRepository lookupOptionRepository;

    public LookupService(LookupOptionRepository lookupOptionRepository) {
        this.lookupOptionRepository = lookupOptionRepository;
    }

    public List<LookupOption> listActive(LookupCategory category) {
        return lookupOptionRepository.findByCategoryAndActiveTrueOrderBySortOrderAscLabelAsc(category);
    }

    public List<LookupOption> listAll(LookupCategory category) {
        return lookupOptionRepository.findByCategoryOrderBySortOrderAscLabelAsc(category);
    }

    public Optional<LookupOption> findByCategoryAndId(LookupCategory category, Long id) {
        if (category == null || id == null) {
            return Optional.empty();
        }
        return lookupOptionRepository.findById(id)
                .filter(opt -> category.equals(opt.getCategory()) && opt.isActive());
    }

    public Optional<LookupOption> findByCategoryAndLabel(LookupCategory category, String label) {
        if (category == null || label == null || label.isBlank()) {
            return Optional.empty();
        }
        return lookupOptionRepository.findByCategoryAndLabelIgnoreCase(category, label.trim())
                .filter(LookupOption::isActive);
    }
}
