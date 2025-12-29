package org.smartsupply.repository;

import org.smartsupply.model.entity.POLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface POLineRepository extends JpaRepository<POLine,Long> {
    List<POLine> findByPurchaseOrderId(Long purchaseOrderId);
}
