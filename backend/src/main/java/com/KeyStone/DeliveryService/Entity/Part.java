package com.KeyStone.DeliveryService.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "parts")
@Getter
@Setter
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(length = 64)
    private String sku;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private Integer stockQuantity;

    public Part() {
    }

    public Part(String name, Integer stockQuantity) {
        this.name = name;
        this.stockQuantity = stockQuantity;
    }
}
