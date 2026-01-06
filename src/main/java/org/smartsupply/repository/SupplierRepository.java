package org.smartsupply.repository;

import org.smartsupply.model.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    boolean existsByEmail(String email);
    boolean existsByName(String name);
    Optional<Supplier> findByEmail(String email);
    List<Supplier> findByNameContainingIgnoreCase(String q);
}