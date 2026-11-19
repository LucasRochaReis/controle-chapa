package br.com.techfiles.controlepapel.repo;

import br.com.techfiles.controlepapel.model.PlateProductionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlateProductionEntryRepository extends JpaRepository<PlateProductionEntry, Long>, JpaSpecificationExecutor<PlateProductionEntry> {
    long countByMachine_Id(Long machineId);
    long countByShift_Id(Long shiftId);
    long countByClientName(String clientName);

    @Query("SELECT DISTINCT p.observations FROM PlateProductionEntry p "
            + "WHERE p.observations IS NOT NULL AND p.observations <> '' "
            + "AND UPPER(p.observations) LIKE UPPER(CONCAT(:prefixo, '%')) "
            + "ORDER BY p.observations ASC")
    List<String> findObservacoesByPrefixo(@Param("prefixo") String prefixo);

    @org.springframework.data.jpa.repository.Query("SELECT MIN(p.entryDate) FROM PlateProductionEntry p")
    java.time.LocalDate findMinEntryDate();

    @org.springframework.data.jpa.repository.Query("SELECT MAX(p.entryDate) FROM PlateProductionEntry p")
    java.time.LocalDate findMaxEntryDate();

    @Modifying
    @Query("UPDATE PlateProductionEntry p SET p.machine.id = :toId WHERE p.machine.id = :fromId")
    int remapMachine(@Param("fromId") Long fromId, @Param("toId") Long toId);

    @Modifying
    @Query("UPDATE PlateProductionEntry p SET p.shift.id = :toId WHERE p.shift.id = :fromId")
    int remapShift(@Param("fromId") Long fromId, @Param("toId") Long toId);
}
