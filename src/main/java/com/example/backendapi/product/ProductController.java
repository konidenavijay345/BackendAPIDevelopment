package com.example.backendapi.product;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;

/**
 * REST controller for authenticated product operations.
 *
 * <p>The {@link Principal} comes from JWT authentication and supplies a trusted identity. Owner
 * IDs are never accepted from request JSON, preventing clients from assigning records to others.</p>
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    /** Creates the HTTP adapter with its product use-case service. */
    public ProductController(ProductService service) {
        this.service = service;
    }

    /** Creates a product for the authenticated user and returns 201 with its resource location. */
    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request,
            UriComponentsBuilder uriBuilder,
            Principal principal
    ) {
        ProductResponse created = service.create(principal.getName(), request);
        URI location = uriBuilder.path("/api/v1/products/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Returns a paginated list containing only the authenticated user's products. */
    @GetMapping
    public Page<ProductResponse> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable, Principal principal
    ) {
        return service.findAll(principal.getName(), pageable);
    }

    /** Returns one owned product or 404 when it is unavailable to this user. */
    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id, Principal principal) {
        return service.findById(principal.getName(), id);
    }

    /** Replaces editable fields on an owned product after input validation. */
    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id, @Valid @RequestBody ProductRequest request, Principal principal
    ) {
        return service.update(principal.getName(), id, request);
    }

    /** Deletes an owned product and returns 204 because no response body is needed. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        service.delete(principal.getName(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
