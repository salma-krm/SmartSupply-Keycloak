package org.smartsupply.repository;

import org.smartsupply.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.smartsupply.model.entity.SalesOrderLine;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, Long> {
    List<SalesOrderLine> findBySalesOrderId(Long salesOrderId);
    List<SalesOrderLine> findByProductId(Long productId);
    long countByProductId(Long productId);
    long countByProduct_SkuAndSalesOrder_StatusIn(String sku, Collection<OrderStatus> statuses);
}
