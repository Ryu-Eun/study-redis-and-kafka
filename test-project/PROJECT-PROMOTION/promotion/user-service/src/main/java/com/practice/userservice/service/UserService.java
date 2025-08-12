package com.practice.userservice.service;

import com.practice.userservice.entity.User;
import com.practice.userservice.entity.UserLoginHistory;
import com.practice.userservice.exception.DuplicateUserException;
import com.practice.userservice.exception.UnauthorizedAccessException;
import com.practice.userservice.exception.UserNotFoundException;
import com.practice.userservice.repository.UserLoginHistoryRepository;
import com.practice.userservice.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserLoginHistoryRepository userLoginHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserLoginHistoryRepository userLoginHistoryRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userLoginHistoryRepository = userLoginHistoryRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public User createUser(String email, String password, String name){
        // email로 이미 있는 유저인지 확인 (email이 unique라서 가능)
        if(userRepository.findByEmail(email).isPresent()){
            throw new DuplicateUserException("User already exists with email: " + email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(name);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User authenticate(String email, String password){
        // 해당 email에 대응되는 유저가 없으면 로그인 실패(UserNotFoundException)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // 비밀번호 일치하지않으면 로그인 실패()
        if(!passwordEncoder.matches(password, user.getPasswordHash())){
            throw new UnauthorizedAccessException("Invalid password");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId){
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    @Transactional
    public User updateUser(Long userId, String name){
        User user = getUserById(userId);
        user.setName(name);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword){
        User user = getUserById(userId);

        if(!passwordEncoder.matches(currentPassword, user.getPasswordHash())){
            throw new UnauthorizedAccessException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserLoginHistory> getUserLoginHistory(Long userId){
        User user = getUserById(userId);
        return userLoginHistoryRepository.findByUserOrderByLoginTimeDesc(user);
    }

}