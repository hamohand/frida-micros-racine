package com.muhend.backendai.controller;

import com.muhend.backendai.config.security.JwtUtils;
import com.muhend.backendai.dto.JwtResponse;
import com.muhend.backendai.dto.LoginRequest;
import com.muhend.backendai.entities.UtilisateurEntity;
import com.muhend.backendai.repository.UtilisateurRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UtilisateurRepo utilisateurRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        return ResponseEntity.ok(new JwtResponse(jwt, userDetails.getUsername(), role));
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<?> registerUser(@RequestBody LoginRequest signUpRequest) {
        if (utilisateurRepo.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Erreur : Le nom d'utilisateur est déjà pris !");
        }

        // Créer un nouveau compte utilisateur
        UtilisateurEntity user = new UtilisateurEntity();
        user.setUsername(signUpRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setRole("ROLE_USER");

        utilisateurRepo.save(user);

        return ResponseEntity.ok("Utilisateur enregistré avec succès !");
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('MAITRE')")
    public ResponseEntity<List<UtilisateurEntity>> getAllUsers() {
        return ResponseEntity.ok(utilisateurRepo.findAll());
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSecurityStatus(@Value("${app.demo-mode:false}") boolean demoMode) {
        return ResponseEntity.ok(java.util.Collections.singletonMap("demoMode", demoMode));
    }
}
