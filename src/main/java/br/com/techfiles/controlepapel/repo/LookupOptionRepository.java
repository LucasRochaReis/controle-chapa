package br.com.techfiles.controlepapel.repo;

import br.com.techfiles.controlepapel.model.LookupCategory;
import br.com.techfiles.controlepapel.model.LookupOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LookupOptionRepository extends JpaRepository<LookupOption, Long> {
    List<LookupOption> findByCategoryAndActiveTrueOrderBySortOrderAscLabelAsc(LookupCategory category);
    List<LookupOption> findByCategoryOrderBySortOrderAscLabelAsc(LookupCategory category);
    List<LookupOption> findAllByOrderByCategoryAscSortOrderAscLabelAsc();
    Optional<LookupOption> findByCategoryAndLabelIgnoreCase(LookupCategory category, String label);
    List<LookupOption> findByCategory(LookupCategory category);
}
