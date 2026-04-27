package com.example.productcrud.controller;

import com.example.productcrud.model.Category;
import com.example.productcrud.model.Product;
import com.example.productcrud.model.User;
import com.example.productcrud.repository.UserRepository;
import com.example.productcrud.service.ProductService;
import com.example.productcrud.service.CategoryService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// TAMBAHAN IMPORT UNTUK PAGINATION
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, UserRepository userRepository, CategoryService categoryService) {
        this.productService = productService;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
    }

    @GetMapping("/")
    public String index() {
        return "index"; // Diubah ke index agar halaman welcome muncul
    }

    @GetMapping("/dashboard")
    public String showDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // Untuk Dashboard, kita bisa menggunakan Pageable agar tidak mengambil semua data sekaligus jika data sangat besar,
        // namun untuk sementara saya ubah agar mengambil Page pertama saja dengan ukuran besar agar grafik tetap jalan.
        User currentUser = getCurrentUser(userDetails);

        // Ambil maksimal 1000 data untuk dihitung di dashboard (ini cara aman tanpa merusak grafik Anda)
        Page<Product> productPage = productService.findAllByOwner(currentUser, PageRequest.of(0, 1000));
        List<Product> products = productPage.getContent();

        long totalProducts = products.size();
        double totalValue = products.stream().mapToDouble(p -> p.getPrice() * p.getStock()).sum();
        long activeCount = products.stream().filter(Product::isActive).count();
        long inactiveCount = totalProducts - activeCount;

        List<Product> lowStock = products.stream().filter(p -> p.getStock() < 5).collect(Collectors.toList());

        Map<String, Long> categoryDist = products.stream()
                .filter(p -> p.getCategory() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getCategory().getName(),
                        Collectors.counting()
                ));

        model.addAttribute("products", products);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("inactiveCount", inactiveCount);
        model.addAttribute("lowStockProducts", lowStock);
        model.addAttribute("categoryDistribution", categoryDist);

        return "dashboard";
    }

    @GetMapping("/products")
    public String listProducts(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "category", required = false) Category category,
                               @RequestParam(value = "page", defaultValue = "0") int page, // Parameter halaman
                               Model model) {
        User currentUser = getCurrentUser(userDetails);

        // MEMBUAT PAGEABLE: Halaman saat ini, 10 item per halaman, urut berdasarkan ID menurun (Terbaru)
        Pageable pageable = PageRequest.of(page, 10, Sort.by("id").descending());

        Page<Product> productPage; // Menggunakan Page, bukan List

        if ((keyword != null && !keyword.isEmpty()) || category != null) {
            productPage = productService.searchProducts(keyword, category, currentUser, pageable);
        } else {
            productPage = productService.findAllByOwner(currentUser, pageable);
        }

        // KIRIM DATA PAGINATION KE HTML
        model.addAttribute("products", productPage.getContent()); // Isi datanya
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());

        model.addAttribute("categories", categoryService.findAllByOwner(currentUser));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);

        return "product/list";
    }

    // --- (SISA METHOD DI BAWAH INI SAMA SEPERTI SEBELUMNYA) ---

    @GetMapping("/products/{id}")
    public String detailProduct(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(userDetails);
        return productService.findByIdAndOwner(id, currentUser)
                .map(product -> {
                    model.addAttribute("product", product);
                    return "product/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Produk tidak ditemukan.");
                    return "redirect:/products";
                });
    }

    @GetMapping("/products/new")
    public String showCreateForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = getCurrentUser(userDetails);
        Product product = new Product();
        product.setCreatedAt(LocalDate.now());
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.findAllByOwner(currentUser));
        return "product/form";
    }

    @GetMapping("/products/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(userDetails);
        return productService.findByIdAndOwner(id, currentUser)
                .map(product -> {
                    model.addAttribute("product", product);
                    model.addAttribute("categories", categoryService.findAllByOwner(currentUser));
                    return "product/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Produk tidak ditemukan.");
                    return "redirect:/products";
                });
    }

    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(userDetails);

        if (product.getId() != null) {
            boolean isOwner = productService.findByIdAndOwner(product.getId(), currentUser).isPresent();
            if (!isOwner) {
                redirectAttributes.addFlashAttribute("errorMessage", "Produk tidak ditemukan.");
                return "redirect:/products";
            }
        }

        product.setOwner(currentUser);
        productService.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Produk berhasil disimpan!");
        return "redirect:/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser(userDetails);
        boolean isOwner = productService.findByIdAndOwner(id, currentUser).isPresent();

        if (isOwner) {
            productService.deleteByIdAndOwner(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Produk berhasil dihapus!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Produk tidak ditemukan.");
        }

        return "redirect:/products";
    }
}