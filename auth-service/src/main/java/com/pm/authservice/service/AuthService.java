package com.pm.authservice.service;

import com.pm.authservice.dto.LoginRequestDTO;
import java.util.Optional;
import com.pm.authservice.model.User;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        Optional<User> user = userService.findByEmail(loginRequestDTO.getEmail());
    }

}
