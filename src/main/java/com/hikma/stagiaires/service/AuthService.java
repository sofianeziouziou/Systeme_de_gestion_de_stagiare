package com.hikma.stagiaires.service;

import com.hikma.stagiaires.dto.auth.AuthDTOs.*;
import com.hikma.stagiaires.model.AccountStatus;
import com.hikma.stagiaires.model.Role;
import com.hikma.stagiaires.model.User;
import com.hikma.stagiaires.repository.UserRepository;
import com.hikma.stagiaires.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse login(LoginRequest req) {
        // 1. Vérifier que l'utilisateur existe
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Email ou mot de passe incorrect"));

        // 2. Vérifier le statut AVANT l'authentification
        if (AccountStatus.EN_ATTENTE.equals(user.getAccountStatus())) {
            throw new IllegalStateException("Votre compte est en attente d'approbation par le RH.");
        }
        if (AccountStatus.REFUSE.equals(user.getAccountStatus())) {
            throw new IllegalStateException("Votre demande de compte a été refusée. Contactez le RH.");
        }

        // 3. Authentifier (vérifie le mot de passe)
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (DisabledException e) {
            throw new IllegalStateException("Compte désactivé. Contactez le RH.");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé.");
        }

        Role role = Role.valueOf(req.getRole().toUpperCase());

        // RH approuvé directement, les autres en attente
        AccountStatus status = role == Role.RH
                ? AccountStatus.APPROUVE
                : AccountStatus.EN_ATTENTE;

        User user = User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .role(role)
                .accountStatus(status)
                .build();

        userRepository.save(user);

        // Pas de token si EN_ATTENTE — juste un message
        if (AccountStatus.EN_ATTENTE.equals(status)) {
            AuthResponse response = new AuthResponse();
            response.setPendingApproval(true);
            response.setMessage("Votre demande a été soumise. En attente d'approbation du RH.");
            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
            userInfo.setId(user.getId());
            userInfo.setEmail(user.getEmail());
            userInfo.setFirstName(user.getFirstName());
            userInfo.setLastName(user.getLastName());
            userInfo.setRole(user.getRole().name());
            userInfo.setAccountStatus(status.name());
            response.setUser(userInfo);
            return response;
        }

        // RH → token immédiat
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return buildAuthResponse(accessToken, refreshToken, user);
    }

    public AuthResponse refreshToken(RefreshTokenRequest req) {
        String email = jwtService.extractUsername(req.getRefreshToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (!jwtService.isTokenValid(req.getRefreshToken(), user)) {
            throw new IllegalArgumentException("Refresh token invalide ou expiré.");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        return buildAuthResponse(newAccessToken, req.getRefreshToken(), user);
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setEmail(user.getEmail());
        userInfo.setFirstName(user.getFirstName());
        userInfo.setLastName(user.getLastName());
        userInfo.setRole(user.getRole().name());
        userInfo.setPhotoUrl(user.getPhotoUrl());
        userInfo.setAccountStatus(user.getAccountStatus().name());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUser(userInfo);
        return response;
    }
}