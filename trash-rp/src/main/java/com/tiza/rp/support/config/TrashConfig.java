package com.tiza.rp.support.config;

import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import kafka.serializer.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Description: TrashConfig
 * Author: DIYILIU
 * Update: 2018-12-06 10:21
 */

@Slf4j
@Configuration
@EnableScheduling
public class TrashConfig {

    @Value("${kafka.broker-list}")
    private String brokerList;

    @Bean
    public Producer kafkaProducer(){
        Map prop = new HashMap();
        // kafka 集群
        prop.put("metadata.broker.list", "xg153:9092,xg154:9092,xg155:9092");
        // 消息传递到broker时的序列化方式
        prop.put("serializer.class", StringEncoder.class.getName());
        // 是否获取反馈
        // 0是不获取反馈(消息有可能传输失败)
        // 1是获取消息传递给leader后反馈(其他副本有可能接受消息失败)
        // -1是所有in-sync replicas接受到消息时的反馈
        prop.put("request.required.acks", "1");
        // 内部发送数据是异步还是同步 sync：同步, 默认 async：异步
        prop.put("producer.type", "async");
        // 重试次数
        prop.put("message.send.max.retries", "3");
        // 异步提交的时候(async)，并发提交的记录数
        prop.put("batch.num.messages", "200");
        // 设置缓冲区大小，默认10KB
        prop.put("send.buffer.bytes", "102400");

        Properties properties = new Properties();
        properties.putAll(prop);
        return new Producer(new ProducerConfig(properties));
    }
}
