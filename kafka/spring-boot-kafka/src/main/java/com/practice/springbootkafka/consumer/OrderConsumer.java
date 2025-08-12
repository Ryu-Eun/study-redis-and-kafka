package com.practice.springbootkafka.consumer;

import com.practice.springbootkafka.domain.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderConsumer {
    @KafkaListener(topics="orders", groupId="order-group")
    public void listen(@Payload OrderEvent order, // 실제 메시지 내용. Kafka에서 받은 JSON을 OrderEvent 객체로 자동 변환
                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition, // 메시지가 저장된 파티션 번호
                       @Header(KafkaHeaders.OFFSET) long offset) { // 파티션 내 메시지 위치
        try{
            log.info("Received order: {}, partition: {}, offset: {}",
                    order.getOrderId(), partition, offset);
            processOrder(order);
            // 자동으로 offset commit
        }catch(Exception e){
            log.error("Error processing order: {}", order.getOrderId(), e );
            handleError(order, e);
            // offset commit 안됨 (재처리 가능 상태)
        }
    }

    protected void processOrder(OrderEvent order){
        //주문 처리 로직
        log.info("Processing order: {}", order.getOrderId());
    }

    private void handleError(OrderEvent order, Exception ex){
        //에러 처리 로직

    }

}