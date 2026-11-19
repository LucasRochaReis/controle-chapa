package br.com.techfiles.controlepapel.repo;

import br.com.techfiles.controlepapel.model.PlateDefectEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlateDefectEntryRepository extends JpaRepository<PlateDefectEntry, Long>, JpaSpecificationExecutor<PlateDefectEntry> {
    long countByMachine_Id(Long machineId);
    long countByShift_Id(Long shiftId);
    long countByClientName(String clientName);

    @Query("SELECT DISTINCT d.observations FROM PlateDefectEntry d "
            + "WHERE d.observations IS NOT NULL AND d.observations <> '' "
            + "AND UPPER(d.observations) LIKE UPPER(CONCAT(:prefixo, '%')) "
            + "ORDER BY d.observations ASC")
    List<String> findObservacoesByPrefixo(@Param("prefixo") String prefixo);

    @org.springframework.data.jpa.repository.Query("SELECT MIN(d.entryDate) FROM PlateDefectEntry d")
    java.time.LocalDate findMinEntryDate();

    @org.springframework.data.jpa.repository.Query("SELECT MAX(d.entryDate) FROM PlateDefectEntry d")
    java.time.LocalDate findMaxEntryDate();

    @Modifying
    @Query("UPDATE PlateDefectEntry d SET d.machine.id = :toId WHERE d.machine.id = :fromId")
    int remapMachine(@Param("fromId") Long fromId, @Param("toId") Long toId);

    @Modifying
    @Query("UPDATE PlateDefectEntry d SET d.shift.id = :toId WHERE d.shift.id = :fromId")
    int remapShift(@Param("fromId") Long fromId, @Param("toId") Long toId);
}
