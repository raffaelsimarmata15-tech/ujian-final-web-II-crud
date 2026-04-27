package com.example.productcrud.controller;

import com.example.productcrud.model.Category;
import com.example.productcrud.model.User;
import com.example.productcrud.repository.UserRepository;
import com.example.productcrud.service.CategoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public CategoryController(CategoryService categoryService, UserRepository userRepository) {
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
    }

    @GetMapping
    public String listCategories(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("categories", categoryService.findAllByOwner(getCurrentUser(userDetails)));
        return "category/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("category", new Category());
        return "category/form";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model, RedirectAttributes redirectAttributes) {
        return categoryService.findByIdAndOwner(id, getCurrentUser(userDetails))
                .map(category -> {
                    model.addAttribute("category", category);
                    return "category/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Kategori tidak ditemukan.");
                    return "redirect:/categories";
                });
    }

    @PostMapping("/save")
    public String saveCategory(@ModelAttribute Category category, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(userDetails);

        // Validasi nama unik
        if (!categoryService.isNameUnique(category.getName(), currentUser, category.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nama kategori sudah ada!");
            // Jika ada id, kembalikan ke edit form, jika tidak ke create form
            return "redirect:/categories/" + (category.getId() != null ? category.getId() + "/edit" : "new");
        }

        category.setOwner(currentUser);
        categoryService.save(category);
        redirectAttributes.addFlashAttribute("successMessage", "Kategori berhasil disimpan!");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteByIdAndOwner(id, getCurrentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "Kategori berhasil dihapus!");
        } catch (Exception e) {
            // Bisa terjadi error jika kategori masih dipakai oleh produk (DataIntegrityViolationException)
            redirectAttributes.addFlashAttribute("errorMessage", "Kategori gagal dihapus. Pastikan tidak ada produk yang menggunakan kategori ini.");
        }
        return "redirect:/categories";
    }
}