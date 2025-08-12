package com.practice.springbootkafka.consumer;

import com.practice.springbootkafka.domain.OrderEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest
class OrderConsumerTest {

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @MockitoSpyBean
    private OrderConsumer consumer;

    @Test
    void testOrderProcessing() {
        // Given
        OrderEvent order = createTestOrder();

        // When
        kafkaTemplate.send("orders", order.getOrderId(), order);

        // Then - processOrder 메서드가 1번 호출 되었는가 + 올바른 order 객체로 호출되었는가
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(consumer, times(1)).processOrder(order)
        );

        // 전체 플로우를 검증 안하는 테스트다.
        // Kafka 직렬화/역직렬화, Consumer Group 설정, Offset 처리, DLT 이동여부 등 성공/실패 케이스 검증이 안됨
    }

    private OrderEvent createTestOrder() {
        List<OrderEvent.OrderItem> items = List.of(new OrderEvent.OrderItem("prod-1", 2, BigDecimal.valueOf(20.00)));
        return new OrderEvent("order-123", "cust-456", items, BigDecimal.valueOf(40.00), LocalDateTime.now());
    }
}