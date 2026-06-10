package com.example.backendapi.product;

import com.example.backendapi.shared.ConflictException;
import com.example.backendapi.shared.ResourceNotFoundException;
import com.example.backendapi.user.AppUser;
import com.example.backendapi.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product application service containing ownership and business rules.
 *
 * <p>This layer separates HTTP concerns from domain operations. Transactions make writes atomic,
 * while owner-scoped repository methods ensure users can operate only on their own data.</p>
 */
@Service
public class ProductService {

    private final ProductRepository repository;
    private final UserService userService;

    /** Injects repository abstractions required by product use cases. */
    public ProductService(ProductRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    /** Creates a product for the user identified by the verified JWT subject. */
    @Transactional
    public ProductResponse create(String email, ProductRequest request) {
        AppUser owner = userService.findByEmail(email);
        String sku = normalizeSku(request.sku());
        if (repository.existsByOwnerIdAndSkuIgnoreCase(owner.getId(), sku)) {
            throw new ConflictException("A product with SKU '" + sku + "' already exists");
        }

        Product product = new Product(
                owner, request.name().trim(), sku, normalizeDescription(request.description()),
                request.price(), request.quantity()
        );
        return ProductResponse.from(repository.save(product));
    }

    /** Retrieves a memory-efficient page of products belonging to the authenticated user. */
    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(String email, Pageable pageable) {
        AppUser owner = userService.findByEmail(email);
        return repository.findAllByOwnerId(owner.getId(), pageable).map(ProductResponse::from);
    }

    /** Retrieves one product through an owner-scoped lookup. */
    @Transactional(readOnly = true)
    public ProductResponse findById(String email, Long id) {
        return ProductResponse.from(getProduct(email, id));
    }

    /** Updates an owned product while preserving per-user SKU uniqueness. */
    @Transactional
    public ProductResponse update(String email, Long id, ProductRequest request) {
        AppUser owner = userService.findByEmail(email);
        Product product = getProduct(owner, id);
        String sku = normalizeSku(request.sku());
        if (repository.existsByOwnerIdAndSkuIgnoreCaseAndIdNot(owner.getId(), sku, id)) {
            throw new ConflictException("A product with SKU '" + sku + "' already exists");
        }

        product.update(
                request.name().trim(), sku, normalizeDescription(request.description()),
                request.price(), request.quantity()
        );
        return ProductResponse.from(repository.save(product));
    }

    /** Deletes a product only after confirming ownership. */
    @Transactional
    public void delete(String email, Long id) {
        repository.delete(getProduct(email, id));
    }

    /** Resolves the user before performing a secure owner-scoped product lookup. */
    private Product getProduct(String email, Long id) {
        return getProduct(userService.findByEmail(email), id);
    }

    /** Centralizes ID-and-owner lookup and deliberately maps inaccessible records to 404. */
    private Product getProduct(AppUser owner, Long id) {
        return repository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " was not found"));
    }

    /** Normalizes SKUs so casing and whitespace cannot bypass uniqueness rules. */
    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }

    /** Stores blank optional descriptions as null for consistent database semantics. */
    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
