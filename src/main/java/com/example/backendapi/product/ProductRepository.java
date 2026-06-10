package com.example.backendapi.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Owner-aware persistence contract for products.
 *
 * <p>Including {@code ownerId} in every business query enforces authorization at the data-access
 * boundary. This prevents accidental cross-user access even if another product ID is supplied.</p>
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
    /** Returns only one owner's products with scalable pagination. */
    Page<Product> findAllByOwnerId(Long ownerId, Pageable pageable);
    /** Finds a product only when both its ID and owner match. */
    Optional<Product> findByIdAndOwnerId(Long id, Long ownerId);
    /** Checks whether an owner already uses a SKU. */
    boolean existsByOwnerIdAndSkuIgnoreCase(Long ownerId, String sku);
    /** Checks SKU uniqueness during update while excluding the current record. */
    boolean existsByOwnerIdAndSkuIgnoreCaseAndIdNot(Long ownerId, String sku, Long id);
}
