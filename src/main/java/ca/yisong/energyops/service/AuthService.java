package ca.yisong.energyops.service;

import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import ca.yisong.energyops.api.ApiModels.AuthResponse;
import ca.yisong.energyops.api.ApiModels.LoginRequest;
import ca.yisong.energyops.api.ApiModels.UserView;
import ca.yisong.energyops.model.UserAccount;
import ca.yisong.energyops.security.AppUserDetailsService;
import ca.yisong.energyops.security.JwtService;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            AppUserDetailsService userDetailsService,
            JwtService jwtService
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        UserAccount account = userDetailsService.findAccount(request.username());
        String token = jwtService.generateToken(userDetails, Map.of("role", account.getRole().name()));
        return new AuthResponse(token, toUserView(account));
    }

    public UserView currentUser(String username) {
        return toUserView(userDetailsService.findAccount(username));
    }

    public UserView toUserView(UserAccount account) {
        return new UserView(
                account.getUsername(),
                account.getFullName(),
                account.getRole().name(),
                account.getHomeProvince()
        );
    }
}
