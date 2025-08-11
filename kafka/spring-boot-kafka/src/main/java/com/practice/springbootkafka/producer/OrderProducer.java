package com.practice.springbootkafka.producer;

import com.practice.springbootkafka.domain.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private static final String TOPIC = "orders";

    // 비동기 전송
    public void sendOrder(OrderEvent order){
        // kafkaTemplate.send()은 CompletableFutre를 리턴한다
        kafkaTemplate.send(TOPIC, order.getOrderId(), order)
                .whenComplete((result, exception) -> {
                    if(exception != null){
                        log.error("Failed to send message: {}", order.getOrderId(), exception);
                    }else{
                        log.info("Message sent successfully: {}, partition: {}",
                                order.getOrderId(), result.getRecordMetadata().partition());
                    }
                });
    }

    // 동기 전송
    public void sendOrderSync(OrderEvent order)throws Exception{
        try{
            SendResult<String, OrderEvent> result = kafkaTemplate.send(TOPIC, order.getOrderId(), order).get();
            log.info("Message sent synchronously: {}, partition: {}",
                    order.getOrderId(), result.getRecordMetadata().partition());
        }catch (Exception e){
            log.error("Error sending message synchronously: {}", e);
            throw e;
        }
    }

}