package com.gymplanner.user;

import com.gymplanner.auth.AuthResult;
import com.gymplanner.auth.AuthService;
import com.gymplanner.common.error.BusinessRuleException;
import com.gymplanner.common.error.NotFoundException;
import com.gymplanner.user.dto.ChangePasswordRequest;
import com.gymplanner.user.dto.UpdateProfileRequest;
import com.gymplanner.user.dto.UserDto;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, AuthService authService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public UserDto getProfile(UUID userId) {
        return UserDto.from(getUser(userId));
    }

    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        user.setDisplayName(request.displayName().trim());
        return UserDto.from(user);
    }

    @Transactional
    public AuthResult changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("invalid_current_password", "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        return authService.reissueAfterPasswordChange(user);
    }

    public User getUser(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User"));
    }
}
