package com.practice.userservice.controller;

import com.practice.userservice.dto.UserDto;
import com.practice.userservice.entity.User;
import com.practice.userservice.entity.UserLoginHistory;
import com.practice.userservice.exception.DuplicateUserException;
import com.practice.userservice.exception.UnauthorizedAccessException;
import com.practice.userservice.exception.UserNotFoundException;
import com.practice.userservice.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 앞서 API gateway에서 X-USER-ID를 추출해줄거임
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> createUser(
            @RequestBody UserDto.SignupRequest request) {
        User user = userService.createUser(request.getEmail(), request.getPassword(), request.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.Response.from(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(
            @RequestHeader("X-USER-ID") Long userId) {
        User user = userService.getUserById(userId);

        return ResponseEntity.ok(UserDto.Response.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody UserDto.UpdateRequest request) {
        User user = userService.updateUser(userId, request.getName());

        return ResponseEntity.ok(UserDto.Response.from(user));
    }

    @PostMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody UserDto.PasswordChangeRequest request) {
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/login-history")
    public ResponseEntity<List<UserLoginHistory>> getLoginHistory(
            @RequestHeader("X-USER-ID") Long userId) {
        List<UserLoginHistory> history = userService.getUserLoginHistory(userId);
        return ResponseEntity.ok(history);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<String> handleDuplicateUser(DuplicateUserException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<String> handleUnauthorizedAccess(UnauthorizedAccessException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }
}