package com.tiza.service.config;

import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.cache.ram.RamCacheProvider;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.service.support.SendConsumer;
import com.tiza.service.support.client.TStarClientAdapter;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.javaapi.consumer.ConsumerConnector;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Description: ServiceConfig
 * Author: DIYILIU
 * Update: 2018-12-11 14:05
 */

@Configuration
@EnableScheduling
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

    @Bean
    public SendConsumer sendConsumer() {
        // 加载数据
        buildData();

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

    /**
     * 加载接口数据
     */
    public void buildData() {
        // 加载接口数据
        try {
            ResourceLoader resourceLoader = new DefaultResourceLoader();
            File file = resourceLoader.getResource("classpath:config/data.json").getFile();

            String data = FileUtils.readFileToString(file);
            Map map = JacksonUtil.toObject(data, HashMap.class);
            callInfoProvider().put(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    public ICache bagOptProvider() {

        return new RamCacheProvider();
    }

    @Bean
    public ICache vehicleInfoProvider() {

        return new RamCacheProvider();
    }

    @Bean
    public ICache callInfoProvider() {

        return new RamCacheProvider();
    }
}
