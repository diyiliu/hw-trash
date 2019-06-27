package com.tiza.trash.rp.support.config;

import com.tiza.trash.rp.support.util.KafkaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Description: TrashConfig
 * Author: DIYILIU
 * Update: 2018-12-06 10:21
 */

@Slf4j
@Configuration
@EnableScheduling
@PropertySource("classpath:config.properties")
public class TrashConfig {

    @Value("${kafka.broker-list}")
    private String brokerList;

    @Bean
    public KafkaUtil kafkaUtil(){
        KafkaUtil kafkaUtil = new KafkaUtil();
        kafkaUtil.setBrokers(brokerList);
        kafkaUtil.init();

        return kafkaUtil;
    }
}
