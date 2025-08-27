package com.practice.pointservicebatch.repository;

import com.practice.pointservicebatch.entity.PointBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointBalanceRepository extends JpaRepository<PointBalance, Long> {
}
