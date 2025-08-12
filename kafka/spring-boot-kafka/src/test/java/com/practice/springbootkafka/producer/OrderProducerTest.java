package com.practice.springbootkafka.producer;

import com.practice.springbootkafka.domain.OrderEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderProducerTest {

    @Autowired
    private OrderProducer orderProducer;

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private Consumer<String, OrderEvent> consumer;

    @BeforeEach
    void setUp(){
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "localhost:9092",
                "test-group",
                "true"
        );

        consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(), // key 역직렬화
                new JsonDeserializer<>(OrderEvent.class) // value 역직렬화
        ).createConsumer();

        consumer.subscribe(List.of("orders"));
        consumer.poll(Duration.ofMillis(100)); // 더미 poll로 파티션 할당받아야됨
        consumer.seekToBeginning(consumer.assignment());
    }

    @AfterEach
    void tearDown(){
        consumer.close();
    }

    @Test
    void testSendOrder(){
        //given
        OrderEvent order = createTestOrder();

        //when
        orderProducer.sendOrder(order);

        //then
        ConsumerRecord<String, OrderEvent> record = KafkaTestUtils.getSingleRecord(consumer, "orders");
        assertThat(record).isNotNull();
        assertThat(record.value().getOrderId()).isEqualTo(order.getOrderId());

    }

    private OrderEvent createTestOrder(){
        List<OrderEvent.OrderItem> items = List.of(new OrderEvent.OrderItem("prod-1",2, BigDecimal.valueOf(20.00)));
        return new OrderEvent("order-123", "cust-456", items, BigDecimal.valueOf(40.00), LocalDateTime.now());
    }
}