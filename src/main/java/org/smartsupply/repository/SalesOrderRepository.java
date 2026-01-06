package org.smartsupply.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.smartsupply.model.entity.SalesOrder;
import org.smartsupply.model.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    List<SalesOrder> findByClientId(Long clientId);


    Page<SalesOrder> findByStatus(OrderStatus status, Pageable pageable);


    Page<SalesOrder> findByClientId(Long clientId, Pageable pageable);


    Page<SalesOrder> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);


    Page<SalesOrder> findByStatusAndClientId(OrderStatus status, Long clientId, Pageable pageable);
    Page<SalesOrder> findByClientIdAndCreatedAtBetween(Long clientId, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<SalesOrder> findByStatusAndCreatedAtBetween(OrderStatus status, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<SalesOrder> findByStatusAndClientIdAndCreatedAtBetween(OrderStatus status, Long clientId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    boolean existsByIdAndStatus(Long id, OrderStatus status);
}