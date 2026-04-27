package com.example.productcrud.service;

import com.example.productcrud.model.Category;
import com.example.productcrud.model.Product;
import com.example.productcrud.model.User;
import com.example.productcrud.repository.ProductRepository;
import org.springframework.data.domain.Page; // Tambahan Import
import org.springframework.data.domain.Pageable; // Tambahan Import
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // DIUBAH: Dari List menjadi Page dan menerima Pageable
    public Page<Product> findAllByOwner(User owner, Pageable pageable) {
        return productRepository.findByOwner(owner, pageable);
    }

    public Optional<Product> findByIdAndOwner(Long id, User owner) {
        return productRepository.findByIdAndOwner(id, owner);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void deleteByIdAndOwner(Long id, User owner) {
        productRepository.findByIdAndOwner(id, owner)
                .ifPresent(product -> productRepository.delete(product));
    }

    // DIUBAH: Dari List menjadi Page dan menerima Pageable
    public Page<Product> searchProducts(String keyword, Category category, User owner, Pageable pageable) {
        return productRepository.searchProducts(keyword, category, owner, pageable);
    }
}