package com.practice.timesaleservice.repository;

import com.practice.timesaleservice.entity.TimeSaleOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeSaleOrderRepository extends JpaRepository<TimeSaleOrder, Long> {
}
