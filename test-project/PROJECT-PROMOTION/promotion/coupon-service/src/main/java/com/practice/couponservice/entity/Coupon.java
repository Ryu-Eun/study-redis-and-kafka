package com.practice.couponservice.entity;

import com.practice.couponservice.exception.CouponAlreadyUsedException;
import com.practice.couponservice.exception.CouponExpiredException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String couponCode;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Long orderId; // nullable
    private LocalDateTime usedAt; // nullable
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id")
    private CouponPolicy couponPolicy;

    public enum Status{
        AVAILABLE,
        USED,
        EXPIRED,
        CANCELLED,
    }

    @Builder
    public Coupon(Long id, CouponPolicy couponPolicy, Long userId, String couponCode){
        this.id = id;
        this.couponPolicy = couponPolicy;
        this.userId = userId;
        this.couponCode = couponCode;
        this.status = Status.AVAILABLE;
    }

    // 쿠폰 사용
    public void use(Long orderId){
        if(status == Status.USED){
            throw new CouponAlreadyUsedException("이미 사용된 쿠폰입니다.");
        }

        if(isExpired()){
            throw new CouponExpiredException("만료된 쿠폰입니다.");
        }

        this.status = Status.USED;
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
    }

    // 쿠폰 취소됨
    public void cancel() {
        if (status != Status.USED) {
            throw new IllegalStateException("사용되지 않은 쿠폰입니다.");
        }
        this.status = Status.CANCELLED;
        this.orderId = null;
        this.usedAt = null;
    }

    // 쿠폰 만료됨
    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime());
    }

    // 쿠폰 사용됨
    public boolean isUsed() {
        return status == Status.USED;
    }


}
