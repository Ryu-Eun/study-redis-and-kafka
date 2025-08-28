package com.practice.timesaleservice.service.v3;


import com.practice.timesaleservice.dto.PurchaseRequestMessage;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 타임세일 구매 요청을 처리하는 Producer
 * - kafka를 통해 비동기로 구매 요청을 처리
 * - Redis를 사용하여 대기열 관리
 * - Redisson을 사용하여 분산 환경에서의 동시성 제어
 */
@Service
@RequiredArgsConstructor
public class TimeSaleProducer {
    // kafka 토픽 이름
    private static final String TOPIC = "time-sale-requests";
    private static final String RESULT_PREFIX = "time-sale-requests:";
    private static final String QUEUE_KEY = "time-sale-queue:";
    private static final String TOTAL_REQUESTS_KEY = "time-sale-total-requests:";

    private final KafkaTemplate<String, PurchaseRequestMessage> kafkaTemplate;
    private final RedissonClient redissonClient;


    /**
     * 타임세일 구매 요청을 처리
     * 1. 요청 id 생성
     * 2. redis에 요청 상태 저장
     * 3. 대기열에 요청 추가
     * 4. kafka에 메시지 전송
     */
    public String sendPurchaseRequest(Long timeSaleId, Long userId, Long quantity){
        // 고유한 요청 ID 생성
        String requestId = UUID.randomUUID().toString();

        // 구매 요청 메시지 생성
        PurchaseRequestMessage message = PurchaseRequestMessage.builder()
                .requestId(requestId)
                .timeSaleId(timeSaleId)
                .userId(userId)
                .quantity(quantity)
                .build();

        // redis에 초기상태 저장
        RBucket<String> resultBucket = redissonClient.getBucket(RESULT_PREFIX + requestId);
        resultBucket.set("PENDING");

        // 대기열에 추가하고 카운터 증가
        String queueKey = QUEUE_KEY + timeSaleId;
        String totalKey = TOTAL_REQUESTS_KEY + timeSaleId;

        RBucket<String> queueBucket = redissonClient.getBucket(queueKey);
        queueBucket.set(requestId);

        RAtomicLong totalCounter = redissonClient.getAtomicLong(totalKey);
        totalCounter.incrementAndGet();

        // kafka로 메시지 전송
        kafkaTemplate.send(TOPIC, requestId, message);
        return requestId;
    }

    /**
     * 대기열에서 요청의 위치를 조회
     */
    public Integer getQueuePosition(Long timeSaleId, String requestId) {
        String queueKey = QUEUE_KEY + timeSaleId;
        RBucket<String> queueBucket = redissonClient.getBucket(queueKey);
        String queueValue = queueBucket.get();

        if(queueValue == null || queueValue.isEmpty()) {
            return null;
        }

        String[] queueValues = queueValue.split(",");
        for(int i = 0; i < queueValues.length; i++) {
            if(requestId.equals(queueValues[i])) {
                return i+1;
            }
        }
        return null;
    }

    /**
     * 총 대기 중인 요청 수를 조회
     */
    public Long getTotalWaiting(Long timeSaleId){
        String totalKey = TOTAL_REQUESTS_KEY + timeSaleId;
        RAtomicLong totalCounter = redissonClient.getAtomicLong(totalKey);
        return totalCounter.get();
    }

}
