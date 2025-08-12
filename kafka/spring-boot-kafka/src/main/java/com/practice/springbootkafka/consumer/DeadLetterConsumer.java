package com.practice.springbootkafka.consumer;

import com.practice.springbootkafka.domain.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeadLetterConsumer {

    @KafkaListener(topics="order-DLT", groupId="dlt-group")
    public void listenDLT(@Payload OrderEvent order,
                          @Header("kafka_exception-fqcn") String exceptionType,
                          @Header("kafka_exception-message") String exceptionMessage) {
        log.error("DLT에서 실패 주문 수신: {}", order.getOrderId());
        log.error("실패 사유: {} - {}", exceptionType, exceptionMessage);
    }
}
