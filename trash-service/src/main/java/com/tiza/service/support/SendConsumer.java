package com.tiza.service.support;

import cn.com.tiza.tstar.datainterface.client.TStarSimpleClient;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.util.HttpUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.rp.support.model.SendData;
import com.tiza.rp.support.parse.process.BagProcess;
import com.tiza.rp.support.parse.process.TrashProcess;
import com.tiza.service.support.task.BagSender;
import com.tiza.service.support.task.TrashSender;
import com.tiza.service.support.task.abs.SendThread;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Description: SendConsumer
 * Author: DIYILIU
 * Update: 2018-12-11 14:12
 */

@Slf4j
public class SendConsumer extends Thread {

    /** 请求票据 **/
    private static String BAG_API_TICKET;
    /** 票据过期时间 **/
    private static long BAG_EXPIRE_TIME;

    private String sendTopic;
    private ConsumerConnector consumer;

    @Resource
    private TrashProcess trashProcess;

    @Resource
    private BagProcess bagProcess;

    @Resource
    private TStarSimpleClient tStarClient;

    @Value("${tstar.terminalType}")
    private String terminalType;

    @Value("${trash.bin.url}")
    private String trashUrl;

    @Value("${trash.bag.url}")
    private String bagUrl;

    /**
     * 应用 ID
     **/
    @Value("${trash.bag.appId}")
    private String bagAppId;

    /**
     * 应用密钥
     **/
    @Value("${trash.bag.appSecret}")
    private String bagSecret;

    @Resource
    private ICache bagOptProvider;

    private final ExecutorService trashThreadPool = Executors.newFixedThreadPool(3);

    private final ExecutorService bagThreadPool = Executors.newFixedThreadPool(3);

    @Override
    public void run() {
        Map<String, Integer> topicCountMap = new HashMap();
        topicCountMap.put(sendTopic, new Integer(1));
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);

        KafkaStream<byte[], byte[]> stream = consumerMap.get(sendTopic).get(0);
        ConsumerIterator<byte[], byte[]> it = stream.iterator();
        while (it.hasNext()) {
            String text = new String(it.next().message());
            log.info("消费 kafka [{}] ... ", text);
            try {
                SendData sendData = JacksonUtil.toObject(text, SendData.class);
                Map data = sendData.getData();
                // 设备类型
                String trashType = (String) data.get("trashType");

                // 垃圾箱
                if ("trash-bin".equals(trashType)) {
                    SendThread sendThread = new TrashSender(tStarClient, trashProcess);
                    sendThread.setTerminalType(terminalType);
                    sendThread.setBaseUrl(trashUrl);
                    sendThread.setData(sendData);

                    trashThreadPool.execute(sendThread);
                    continue;
                }

                // 发放袋
                if ("trash-bag".equals(trashType)) {
                    // 获取公共票据
                    String ticket = getTicket();
                    if (StringUtils.isEmpty(ticket)){
                        log.error("获取 API票据失败!");
                        continue;
                    }

                    BagSender bagSender = new BagSender(tStarClient, bagProcess);
                    bagSender.setTerminalType(terminalType);
                    bagSender.setBaseUrl(bagUrl);
                    bagSender.setData(sendData);
                    // 扩展参数
                    bagSender.setTicket(ticket);
                    bagSender.setBagOptProvider(bagOptProvider);

                    bagThreadPool.execute(bagSender);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setSendTopic(String sendTopic) {
        this.sendTopic = sendTopic;
    }

    public void setConsumer(ConsumerConnector consumer) {
        this.consumer = consumer;
    }

    /**
     * 获取垃圾发放机第三方 API 票据
     * @return
     * @throws Exception
     */
    public String getTicket() throws Exception {
        if (System.currentTimeMillis() > BAG_EXPIRE_TIME) {
            Map param = new HashMap();
            param.put("action", "ticket");
            param.put("appid", bagAppId);
            param.put("secret", bagSecret);

            String json = HttpUtil.getForString(bagUrl, param);
            Map map = JacksonUtil.toObject(json, HashMap.class);
            int errcode = (int) map.get("errcode");
            if (0 == errcode) {
                BAG_API_TICKET = (String) map.get("ticket");
                BAG_EXPIRE_TIME = System.currentTimeMillis() + (int) map.get("expires") * 1000;
            }
        }

        return BAG_API_TICKET;
    }
}
