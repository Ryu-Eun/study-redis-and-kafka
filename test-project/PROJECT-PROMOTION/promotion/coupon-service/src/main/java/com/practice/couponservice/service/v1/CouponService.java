package com.practice.couponservice.service.v1;

import com.practice.couponservice.config.UserIdInterceptor;
import com.practice.couponservice.dto.v1.CouponDto;
import com.practice.couponservice.entity.Coupon;
import com.practice.couponservice.entity.CouponPolicy;
import com.practice.couponservice.exception.CouponIssueException;
import com.practice.couponservice.exception.CouponNotFoundException;
import com.practice.couponservice.repository.CouponPolicyRepository;
import com.practice.couponservice.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public Coupon issueCoupon(CouponDto.IssueRequest request) {

        CouponPolicy couponPolicy = couponPolicyRepository.findByIdWithLock(request.getCouponPolicyId())
                .orElseThrow(() -> new CouponIssueException("쿠폰 정책을 찾을 수 없습니다."));

        // 현재 쿠폰 발급 일자에 해당하는지 체크
        LocalDateTime now = LocalDateTime.now();
        if(now.isBefore(couponPolicy.getStartTime()) || now.isAfter(couponPolicy.getEndTime())){
            throw new CouponIssueException("쿠폰 발급 기간이 아닙니다.");
        }

        // 갯수가 소진됐는지 체크
        long issuedCouponCount = couponRepository.countByCouponPolicyId(couponPolicy.getId());
        if(issuedCouponCount >= couponPolicy.getTotalQuantity()){
            throw new CouponIssueException("쿠폰이 모두 소진되었습니다.");
        }

        Coupon coupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .userId(UserIdInterceptor.getCurrentUserId()) // 유저 정보는 X-USER-ID 헤더로부터 가져온다
                .couponCode(generateCouponCode())
                .build();

        return couponRepository.save(coupon);
    }

    @Transactional
    public Coupon useCoupon(Long couponId, Long orderId){
        Long currentUserId = UserIdInterceptor.getCurrentUserId(); // 유저 정보는 X-USER-ID 헤더로부터 가져온다

        // 해당 쿠폰을 발급받은 유저가 맞는지도 확인해야하기 때문에 유저정보, 쿠폰정보 둘 다 필요하다
        Coupon coupon = couponRepository.findByIdAndUserId(couponId, currentUserId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없거나 접근 권한이 없습니다."));

        coupon.use(orderId);
        return coupon;
    }

    @Transactional
    public Coupon cancelCoupon(Long couponId){
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        Coupon coupon = couponRepository.findByIdAndUserId(couponId, currentUserId)
                .orElseThrow(() -> new CouponNotFoundException("쿠폰을 찾을 수 없거나 접근 권한이 없습니다."));

        coupon.cancel();
        return coupon;
    }

    @Transactional(readOnly = true)
    public Page<Coupon> getCoupons(CouponDto.ListRequest request){
        Long currentUserId = UserIdInterceptor.getCurrentUserId();

        return couponRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                currentUserId,
                request.getStatus(),
                PageRequest.of(
                        request.getPage() != null ? request.getPage() : 0,
                        request.getSize() != null ? request.getSize() : 10
                )
        );

    }



    private String generateCouponCode() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

}