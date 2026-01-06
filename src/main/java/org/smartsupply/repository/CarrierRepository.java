package org.smartsupply.repository;

import org.smartsupply.model.entity.Carrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CarrierRepository extends JpaRepository<Carrier,Long> {

    boolean existsByName(String name);
    Optional<Carrier> findById(Long id);
    Optional<Carrier> findByName(String name);
    Optional<Carrier> findByEmail(String email);
    boolean existsByNameAndIdNot(String name, Long id);
    @Query("SELECT c FROM Carrier c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Carrier> search(@Param("q") String query);
}
