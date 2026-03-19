package ca.yisong.energyops.controller;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.yisong.energyops.api.ApiModels.AuthResponse;
import ca.yisong.energyops.api.ApiModels.LoginRequest;
import ca.yisong.energyops.service.AuthService;
import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public Object currentUser(Authentication authentication) {
        return authService.currentUser(authentication.getName());
    }
}
