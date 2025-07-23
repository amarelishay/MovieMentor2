package movieMentor.services;

import lombok.RequiredArgsConstructor;
import movieMentor.beans.User;
import movieMentor.dto.AuthResponse;
import movieMentor.dto.LoginRequest;
import movieMentor.dto.RegisterRequest;
import movieMentor.repository.UserRepository;
import movieMentor.security.JwtService;
import movieMentor.security.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Attempting to login user: {}", request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails.getUsername());

        log.info("User {} logged in successfully", userDetails.getUsername());
        return new AuthResponse(token);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting to register user: {}", request.getUsername());
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("Registration failed: Email {} already in use", request.getEmail());
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user.getUsername());

        log.info("User {} registered successfully", user.getUsername());
        return new AuthResponse(token);
    }

    @Override
    public void logout() {
        log.info("User logged out (handled at frontend by deleting token)");
    }
}
