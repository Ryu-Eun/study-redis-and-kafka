package com.practice.pointservicebatch.repository;

import com.practice.pointservicebatch.entity.PointBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRepository extends JpaRepository<PointBalance, Long> {
}
