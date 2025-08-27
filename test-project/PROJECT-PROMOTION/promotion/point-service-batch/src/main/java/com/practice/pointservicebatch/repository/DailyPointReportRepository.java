package com.practice.pointservicebatch.repository;

import com.practice.pointservicebatch.entity.DailyPointReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyPointReportRepository extends JpaRepository<DailyPointReport, Long> {
}
