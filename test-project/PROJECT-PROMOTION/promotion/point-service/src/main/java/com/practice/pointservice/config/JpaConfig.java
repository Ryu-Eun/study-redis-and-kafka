package com.practice.pointservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig { //@EntityListeners(AuditingEntityListener.class)사용하기 위함

}
