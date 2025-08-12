package com.practice.springbootkafka.config;

import com.practice.springbootkafka.domain.OrderEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaRetryConfig {

    @Bean(name = "kafkaRetryListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, OrderEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, OrderEvent> consumerFactory, KafkaTemplate<String, OrderEvent> kafkaTemplate
    ){
        ConcurrentKafkaListenerContainerFactory<String, OrderEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // DefaultErrorHandler가 Spring Kafka 2.8 이상에서 기본 에러 핸들러
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), // 실패한 메시지를 Dead Letter Topic으로 전송
                new FixedBackOff(1000L, 3) // Exception 발생했을 때, 재시도 간격과 재시도 횟수 설정
        ));

        return factory;
    }

}
