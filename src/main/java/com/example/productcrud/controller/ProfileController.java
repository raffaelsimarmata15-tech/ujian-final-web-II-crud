package com.example.productcrud.controller;

import com.example.productcrud.model.User;
import com.example.productcrud.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        model.addAttribute("user", user);
        return "profile";
    }

    // ==========================================
    // RUTE BARU: Menampilkan halaman Edit Profil
    // ==========================================
    @GetMapping("/profile/edit")
    public String showEditProfileForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));
        model.addAttribute("user", user);
        return "profile-edit";
    }

    // ==========================================
    // RUTE BARU: Memproses form Edit Profil
    // ==========================================
    @PostMapping("/profile/edit")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @ModelAttribute User updatedUser,
                                RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        // Update data profil (kecuali username dan password)
        user.setFullName(updatedUser.getFullName());
        user.setEmail(updatedUser.getEmail());
        user.setPhoneNumber(updatedUser.getPhoneNumber());
        user.setAddress(updatedUser.getAddress());
        user.setBio(updatedUser.getBio());
        user.setProfileImageUrl(updatedUser.getProfileImageUrl());

        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Profil berhasil diperbarui!");
        return "redirect:/profile";
    }

    @GetMapping("/profile/change-password")
    public String showChangePasswordForm() {
        return "change-password";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes redirectAttributes) {

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

        // 1. Validasi: Password lama harus cocok
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password lama tidak sesuai!");
            return "redirect:/profile/change-password";
        }

        // 2. Validasi: Password baru dan konfirmasi harus sama
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Konfirmasi password baru tidak cocok!");
            return "redirect:/profile/change-password";
        }

        // 3. Encode dan Simpan
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage", "Password berhasil diperbarui!");
        return "redirect:/profile";
    }
}