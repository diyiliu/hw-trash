package com.tiza.service.config;

import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.cache.ram.RamCacheProvider;
import com.tiza.service.support.SendConsumer;
import com.tiza.service.support.client.TStarClientAdapter;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.javaapi.consumer.ConsumerConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.File;
import java.util.Properties;

/**
 * Description: ServiceConfig
 * Author: DIYILIU
 * Update: 2018-12-11 14:05
 */

@Configuration
@PropertySource("classpath:config.properties")
public class ServiceConfig {

    @Value("${env}")
    private String env;

    @Value("${zk.zkConnect}")
    private String zkConnect;

    @Value("${tstar.username}")
    private String tstarUser;

    @Value("${tstar.password}")
    private String tstarPwd;

    @Bean(initMethod = "init")
    public SendConsumer sendConsumer() {
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

        // 消费 topic
        SendConsumer sendConsumer = new SendConsumer();
        sendConsumer.setConsumer(consumer);

        return sendConsumer;
    }

    @Bean(initMethod = "init")
    public TStarClientAdapter tStarClient() {
        return new TStarClientAdapter(tstarUser, tstarPwd, "config" + File.separator + env + File.separator);
    }

    @Bean
    public ICache bagOptProvider() {

        return new RamCacheProvider();
    }

    @Bean
    public ICache callInfoProvider() {

        return new RamCacheProvider();
    }
}
