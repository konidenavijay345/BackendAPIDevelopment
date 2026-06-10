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

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

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

    @GetMapping
    public Page<ProductResponse> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable, Principal principal
    ) {
        return service.findAll(principal.getName(), pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable Long id, Principal principal) {
        return service.findById(principal.getName(), id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id, @Valid @RequestBody ProductRequest request, Principal principal
    ) {
        return service.update(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        service.delete(principal.getName(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
