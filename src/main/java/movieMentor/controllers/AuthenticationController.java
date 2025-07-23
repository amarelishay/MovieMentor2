package movieMentor.controllers;

import lombok.RequiredArgsConstructor;
import movieMentor.dto.AuthResponse;
import movieMentor.dto.LoginRequest;
import movieMentor.dto.RegisterRequest;
import movieMentor.services.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        log.info("Login endpoint called for username: {}", request.getUsername());
        return authService.login(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        log.info("Register endpoint called for username: {}", request.getUsername());
        return authService.register(request);
    }

    @PostMapping("/logout")
    public void logout() {
        log.info("Logout endpoint called");
        authService.logout();
    }
}
