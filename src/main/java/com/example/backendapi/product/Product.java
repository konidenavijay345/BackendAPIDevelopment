package com.example.backendapi.product;

import com.example.backendapi.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Product aggregate persisted in MySQL.
 *
 * <p>The entity owns its update behavior instead of exposing public setters. This encapsulation
 * keeps state changes intentional. The owner association is the foundation of tenant isolation:
 * every product belongs to exactly one authenticated user.</p>
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA when hydrating an entity from the database. */
    protected Product() {
    }

    /** Creates a product and establishes its immutable owner association. */
    public Product(AppUser owner, String name, String sku, String description, BigDecimal price, Integer quantity) {
        this.owner = owner;
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
    }

    /** Changes the editable business fields while ownership remains unchanged. */
    public void update(String name, String sku, String description, BigDecimal price, Integer quantity) {
        this.name = name;
        this.sku = sku;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
    }

    /** Initializes audit timestamps before insertion. */
    @jakarta.persistence.PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Refreshes the modification timestamp before an update statement. */
    @jakarta.persistence.PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    /** Returns the database identifier. */
    public Long getId() { return id; }
    /** Returns the user who owns and may operate on the product. */
    public AppUser getOwner() { return owner; }
    /** Returns the product name. */
    public String getName() { return name; }
    /** Returns the owner's normalized stock-keeping unit. */
    public String getSku() { return sku; }
    /** Returns optional descriptive text. */
    public String getDescription() { return description; }
    /** Returns the precise decimal price; BigDecimal avoids currency rounding errors. */
    public BigDecimal getPrice() { return price; }
    /** Returns current inventory quantity. */
    public Integer getQuantity() { return quantity; }
    /** Returns the creation audit timestamp. */
    public Instant getCreatedAt() { return createdAt; }
    /** Returns the latest modification timestamp. */
    public Instant getUpdatedAt() { return updatedAt; }
}
