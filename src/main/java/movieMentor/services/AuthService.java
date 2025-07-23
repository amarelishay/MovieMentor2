package movieMentor.services;

import movieMentor.dto.AuthResponse;
import movieMentor.dto.LoginRequest;
import movieMentor.dto.RegisterRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(RegisterRequest request);
    void logout();
}
