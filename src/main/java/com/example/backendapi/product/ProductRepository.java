package com.example.backendapi.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findAllByOwnerId(Long ownerId, Pageable pageable);
    Optional<Product> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByOwnerIdAndSkuIgnoreCase(Long ownerId, String sku);
    boolean existsByOwnerIdAndSkuIgnoreCaseAndIdNot(Long ownerId, String sku, Long id);
}
