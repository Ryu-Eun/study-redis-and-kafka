package com.practice.couponservice.service.v2;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.couponservice.dto.v1.CouponPolicyDto;
import com.practice.couponservice.entity.CouponPolicy;
import com.practice.couponservice.exception.CouponPolicyNotFoundException;
import com.practice.couponservice.repository.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


// redis에 캐싱해서 속도 향상
@Service("couponPolicyServiceV2")
@RequiredArgsConstructor
@Slf4j
public class CouponPolicyService {
    private final CouponPolicyRepository couponPolicyRepository;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:";
    private static final String COUPON_POLICY_KEY = "coupon:policy:";

    @Transactional
    public CouponPolicy createCouponPolicy(CouponPolicyDto.CreateRequest request) throws JsonProcessingException {
        CouponPolicy couponPolicy = request.toEntity();
        CouponPolicy savedPolicy = couponPolicyRepository.save(couponPolicy);

        // Redis에 초기 수량 설정
        String quantityKey = COUPON_QUANTITY_KEY + savedPolicy.getId();
        RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
        atomicQuantity.set(savedPolicy.getTotalQuantity());

        // Redis에 정책 정보 저장
        String policyKey = COUPON_POLICY_KEY + savedPolicy.getId();
        String policyJson = objectMapper.writeValueAsString(CouponPolicyDto.Response.from(savedPolicy));
        RBucket<String> bucket = redissonClient.getBucket(policyKey);
        bucket.set(policyJson);

        return savedPolicy;
    }

    public CouponPolicy getCouponPolicy(Long id) {
        String policyKey = COUPON_POLICY_KEY + id;
        RBucket<String> bucket = redissonClient.getBucket(policyKey);
        // Redis에서 정책 먼저 조회 (캐시 우선)
        String policyJson = bucket.get();

        if (policyJson != null) {
            try {
                return objectMapper.readValue(policyJson, CouponPolicy.class);
            } catch (JsonProcessingException e) { // 캐시에 없거나 파싱 실패
                log.error("쿠폰 정책 정보를 JSON으로 파싱하는 중 오류가 발생했습니다.", e);
            }
        }

        return couponPolicyRepository.findById(id)
                .orElseThrow(() -> new CouponPolicyNotFoundException("쿠폰 정책을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<CouponPolicy> getAllCouponPolicies() {
        return couponPolicyRepository.findAll();
    }



}
