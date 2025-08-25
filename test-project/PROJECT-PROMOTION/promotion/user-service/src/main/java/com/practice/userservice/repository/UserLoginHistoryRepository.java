package com.practice.userservice.repository;

import com.practice.userservice.entity.User;
import com.practice.userservice.entity.UserLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLoginHistoryRepository extends JpaRepository<UserLoginHistory, Long> {
    List<UserLoginHistory> findByUserOrderByLoginTimeDesc(User user);
}
