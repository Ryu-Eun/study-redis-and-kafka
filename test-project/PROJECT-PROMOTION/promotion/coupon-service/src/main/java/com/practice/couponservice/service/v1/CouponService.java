package com.practice.couponservice.service.v1;

import com.practice.couponservice.aop.CouponMetered;
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

/**
 * 1.Race Condition 발생 가능성
 * findByIdWithLock으로 쿠폰 정책에 대해 락을 걸지만, countByCouponPolicyId와 실재 쿠폰 저장 사이에 갭이 존재
 * 여러 트랜잭션이 동시에 카운트를 조회하고 조건을 통과한 후 쿠폰을 저장할 수 있음
 * 그래서 결과적으로 totalQuantity보다 더 많은 쿠폰이 발급될 수 있음
 *
 * 2.성능 이슈
 * 매 요청마다 발급된 쿠폰 수를 카운트하는 쿼리 실행
 * 쿠폰 수가 많아질수록 카운트 쿼리의 성능이 저하될 수 있음
 * PESSIMISTIC LOCK으로 인한 병목 현상 발생 가능
 *
 * 3.Dead Lock 가능성
 * 여러 트랜잭션이 동시에 같은 쿠폰에 대해 락을 획득하려 할 때
 * 트랜잭션 타임아웃이 발생할 수 있음
 *
 * 4.정확한 수량 보장의 어려움
 * 분산 환경에서 여러 서버가 동시에 쿠폰을 발급할 경우
 * DB 레벨의 락만으로는 정확한 수량 제어가 어려움
 *
 */
@Service
@RequiredArgsConstructor
public class CouponService {


    private final CouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;


    @Transactional
    @CouponMetered(version = "v1")
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