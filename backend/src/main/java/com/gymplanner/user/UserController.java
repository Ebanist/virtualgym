package com.gymplanner.user;

import com.gymplanner.auth.AuthResult;
import com.gymplanner.auth.AuthUser;
import com.gymplanner.auth.RefreshTokenCookies;
import com.gymplanner.auth.dto.AuthResponse;
import com.gymplanner.user.dto.ChangePasswordRequest;
import com.gymplanner.user.dto.UpdateProfileRequest;
import com.gymplanner.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Profile")
public class UserController {

    private final UserService userService;
    private final RefreshTokenCookies cookies;

    public UserController(UserService userService, RefreshTokenCookies cookies) {
        this.userService = userService;
        this.cookies = cookies;
    }

    @GetMapping
    public UserDto me(AuthUser authUser) {
        return userService.getProfile(authUser.id());
    }

    @PatchMapping
    public UserDto updateProfile(AuthUser authUser, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authUser.id(), request);
    }

    @PostMapping("/password")
    @Operation(summary = "Zmiana hasła – unieważnia pozostałe sesje i zwraca nowe tokeny")
    public ResponseEntity<AuthResponse> changePassword(AuthUser authUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        AuthResult result = userService.changePassword(authUser.id(), request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(result.refreshToken()))
                .body(result.response());
    }
}
