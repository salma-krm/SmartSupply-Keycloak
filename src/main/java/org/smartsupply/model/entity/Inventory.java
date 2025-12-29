package org.smartsupply.model.entity;

import jakarta.persistence.*;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}) })
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Min(0)
    @Column(nullable = false)
    private Integer qtyOnHand ;

    @Min(0)
    @Column(nullable = false)
    private Integer qtyReserved ;

    @OneToMany(mappedBy = "inventory")
    @Builder.Default
    private List<InventoryMovement> movements = new ArrayList<>();

}
