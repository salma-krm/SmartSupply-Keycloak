package org.smartsupply.repository;

import org.smartsupply.model.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByCode(String code);
    boolean existsByCode(String code);
    @Query("SELECT w.id FROM Warehouse w WHERE w.id <> :excludedId")
    List<Long> findAllIdsExcept(@Param("excludedId") Long excludedId);
}