package br.com.techfiles.controlepapel.repo;

import br.com.techfiles.controlepapel.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsernameIgnoreCase(String username);
    Optional<AppUser> findById(Long id);
    boolean existsByUsernameIgnoreCase(String username);
    List<AppUser> findAllByOrderByNameAsc();
    long countByMachine_Id(Long machineId);

    @Modifying
    @Query("UPDATE AppUser u SET u.machine.id = :toId WHERE u.machine.id = :fromId")
    int remapMachine(@Param("fromId") Long fromId, @Param("toId") Long toId);
}
