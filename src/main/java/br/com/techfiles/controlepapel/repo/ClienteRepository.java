package br.com.techfiles.controlepapel.repo;

import br.com.techfiles.controlepapel.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    Optional<Cliente> findByNome(String nome);

    boolean existsByNome(String nome);

    @Query("select c from Cliente c where upper(c.nome) like upper(concat(:prefixo, '%')) order by c.nome asc")
    List<Cliente> findByPrefixo(@Param("prefixo") String prefixo);

    List<Cliente> findAllByOrderByNomeAsc();
}
