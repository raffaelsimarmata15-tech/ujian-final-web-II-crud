package com.example.productcrud.repository;

import com.example.productcrud.model.Category;
import com.example.productcrud.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Dibutuhkan untuk findAllByOwner
    List<Category> findAllByOwnerOrderByNameAsc(User owner);

    // Dibutuhkan untuk findByIdAndOwner
    Optional<Category> findByIdAndOwner(Long id, User owner);

    // Dibutuhkan untuk isNameUnique saat TAMBAH
    boolean existsByNameAndOwner(String name, User owner);

    // Dibutuhkan untuk isNameUnique saat EDIT
    boolean existsByNameAndOwnerAndIdNot(String name, User owner, Long id);
}