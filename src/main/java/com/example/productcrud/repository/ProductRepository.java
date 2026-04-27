package com.example.productcrud.repository;

import com.example.productcrud.model.Category;
import com.example.productcrud.model.Product;
import com.example.productcrud.model.User;
import org.springframework.data.domain.Page; // Tambahan Import
import org.springframework.data.domain.Pageable; // Tambahan Import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // DIUBAH: Dari List menjadi Page dan tambah Pageable
    Page<Product> findByOwner(User owner, Pageable pageable);

    Optional<Product> findByIdAndOwner(Long id, User owner);

    // DIUBAH: Dari List menjadi Page dan tambah Pageable
    @Query("SELECT p FROM Product p WHERE p.owner = :owner " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:category IS NULL OR p.category = :category)")
    Page<Product> searchProducts(@Param("keyword") String keyword,
                                 @Param("category") Category category,
                                 @Param("owner") User owner,
                                 Pageable pageable);
}