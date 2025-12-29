package org.smartsupply.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.smartsupply.model.enums.MovementType;

import java.time.LocalDateTime;

@Entity
@Table(name="inventory_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    @Min(1)
    @Column(nullable = false)
    private int qty;

    @Column(nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    @Column(name = "reference", length = 200)
    private String reference;
    @ManyToOne
    @JoinColumn(name="inventory_id",nullable = false)
    private Inventory inventory;

}
