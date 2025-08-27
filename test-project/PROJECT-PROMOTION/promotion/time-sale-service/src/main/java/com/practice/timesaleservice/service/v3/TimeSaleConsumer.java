package com.practice.timesaleservice.service.v3;


import com.practice.timesaleservice.dto.PurchaseRequestMessage;
import com.practice.timesaleservice.entity.TimeSale;
import com.practice.timesaleservice.entity.TimeSaleOrder;
import com.practice.timesaleservice.repository.TimeSaleOrderRepository;
import com.practice.timesaleservice.repository.TimeSaleRepository;
import com.practice.timesaleservice.service.v1.TimeSaleService;
import com.practice.timesaleservice.service.v2.TimeSaleRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 타임세일 구매 요청을 처리하는 Consumer
 * - kafka를 통해 비동기로 전달된 구매 요청을 처리
 * - redis의 재고를 감소시키고 주문을 생성
 * - 대기열에서 처리된 요청을 제거
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSaleConsumer {
    private final TimeSaleRedisService timeSaleRedisService;
    private final TimeSaleOrderRepository timeSaleOrderRepository;
    private final TimeSaleRepository timeSaleRepository;
    private final RedissonClient redissonClient;

    private static final String RESULT_PREFIX = "time-sale-requests:";
    private static final String QUEUE_KEY = "time-sale-queue:";
    private static final String TOTAL_REQUESTS_KEY = "time-sale-total-requests:";


    /**
     * kafka로부터 수신한 구매 요청을 처리
     * 1. redis에서 타임세일 정보 조회
     * 2. 재고 감소
     * 3. 주문 생성
     * 4. 결과 저장
     * 5. 대기열에서 제거
     */
    @Transactional
    @KafkaListener(topics = "time-sale-requests", groupId = "time-sale-group")
    public void consumePurchaseRequest(PurchaseRequestMessage message) {
        try{
            // db에서 타임세일 정보 조회 및 재고 감소
            TimeSale timeSale = timeSaleRepository.findById(message.getTimeSaleId())
                    .orElseThrow(() -> new IllegalArgumentException("TimeSale not found"));
            timeSale.purchase(message.getQuantity());

            // db에 변경사항 저장
            timeSale = timeSaleRepository.save(timeSale);

            // reids에 변경사항 저장
            timeSaleRedisService.saveToRedis(timeSale);

            // 주문 생성 및 저장
            TimeSaleOrder order = TimeSaleOrder.builder()
                    .userId(message.getUserId())
                    .timeSale(timeSale)
                    .quantity(message.getQuantity())
                    .discountPrice(timeSale.getDiscountPrice())
                    .build();

            TimeSaleOrder savedOrder = timeSaleOrderRepository.save(order);
            savedOrder.complete();

            // 성공 결과 저장
            savedPurchaseResult(message.getRequestId(), "SUCCESS");
        } catch (Exception e) {
            log.error("Failed to process purchase request", message, e);
            // 실패 결과 저장
            savedPurchaseResult(message.getRequestId(), "FAIL");
        } finally {
            removeFromQueue(message.getTimeSaleId(), message.getRequestId());
        }
    }

    /**
     * 구매 요청의 처리 결과를 redis에 저장
     */
    private void savedPurchaseResult(String requestId, String result) {
        RBucket<String> resultBucket = redissonClient.getBucket(RESULT_PREFIX + requestId);
        resultBucket.set(result);

    }

    /**
     * 대기열에서 처리 완료된 요청을 제거
     * 1. 대기열에서 요청 ID 제거
     * 2. 총 대기 수 감소
     */
    private void removeFromQueue(Long timeSaleId, String requestId) {

        try {
            // 대기열에서 요청 제거
            String queueKey = QUEUE_KEY + timeSaleId;
            RBucket<String> queueBucket = redissonClient.getBucket(queueKey);
            String queueValue = queueBucket.get();

            if (queueValue != null || !queueValue.isEmpty()) {
                // 콤마로 구분된 문자열에서 해당 요청 ID 제거
                String[] queueValues = queueValue.split(",");
                StringBuilder newQueue = new StringBuilder();
                for (String value : queueValues) {
                    if (!requestId.equals(value)) {
                        if (!newQueue.isEmpty()) {
                            newQueue.append(",");
                        }
                        newQueue.append(value);
                    }
                }
                queueBucket.set(newQueue.toString());
            }

            //총 대기열 감소
            String totalKey = TOTAL_REQUESTS_KEY + timeSaleId;
            RAtomicLong totalCounter = redissonClient.getAtomicLong(totalKey);
            totalCounter.decrementAndGet();
        }catch (Exception e) {
            log.error("Failed to remove request from queue: timeSaleId={}, requestId={}", timeSaleId, requestId, e);
        }
    }



}
