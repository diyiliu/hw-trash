package com.tiza.service.config;

import cn.com.tiza.tstar.datainterface.client.TStarSimpleClient;
import cn.com.tiza.tstar.datainterface.client.TStarStandardClient;
import com.tiza.service.support.SendConsumer;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.javaapi.consumer.ConsumerConnector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.util.Properties;

/**
 * Description: ServiceConfig
 * Author: DIYILIU
 * Update: 2018-12-11 14:05
 */

@Configuration
@PropertySource("classpath:config.properties")
public class ServiceConfig {

    @Resource
    private Environment environment;

    @Bean
    public SendConsumer sendConsumer(){
        String zkConnect = environment.getProperty("zk.zkConnect");
        String topic = environment.getProperty("kafka.sendTopic");

        String terminalType = environment.getProperty("tstaer.terminalType");

        // kafka 配置
        Properties props = new Properties();
        props.put("zookeeper.connect", zkConnect);
        // 指定消费组名
        props.put("group.id", "g1");
        props.put("zookeeper.session.timeout.ms", "400");
        props.put("zookeeper.sync.time.ms", "200");
        props.put("auto.commit.interval.ms", "1000");
        // 设置消费位置
        props.put("auto.offset.reset", "smallest");

        ConsumerConfig consumerConfig = new ConsumerConfig(props);
        ConsumerConnector consumer = Consumer.createJavaConsumerConnector(consumerConfig);

        // 消费topic
        SendConsumer sendConsumer = new SendConsumer();
        sendConsumer.setSendTopic(topic);
        sendConsumer.setConsumer(consumer);
        sendConsumer.setTerminalType(terminalType);
        sendConsumer.start();

        return sendConsumer;
    }

    @Bean
    public TStarSimpleClient tStarClient() throws Exception{
        String username = environment.getProperty("tstar.username");
        String password = environment.getProperty("tstar.passowrd");

        return new TStarSimpleClient(username, password);
    }
}
