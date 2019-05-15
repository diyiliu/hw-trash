package com.tiza.service.support;

import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.rp.support.model.SendData;
import com.tiza.rp.support.parse.process.BagProcess;
import com.tiza.rp.support.parse.process.TrashProcess;
import com.tiza.service.support.client.TStarClientAdapter;
import com.tiza.service.support.model.CallInfo;
import com.tiza.service.support.task.BagSender;
import com.tiza.service.support.task.TrashSender;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Description: SendConsumer
 * Author: DIYILIU
 * Update: 2018-12-11 14:12
 */

@Slf4j
public class SendConsumer extends Thread {
    private ConsumerConnector consumer;

    @Value("${env}")
    private String env;

    @Value("${kafka.sendTopic}")
    private String sendTopic;

    @Value("${tstar.protocol-jt808}")
    private String protocolJt808;

    @Value("${tstar.protocol-gb32960}")
    private String protocolGb32960;

    @Resource
    private TrashProcess trashProcess;

    @Resource
    private BagProcess bagProcess;

    @Resource
    private TStarClientAdapter tStarClient;

    @Resource
    private ICache callInfoProvider;

    @Resource
    private ICache bagOptProvider;

    private final ExecutorService trashThreadPool = Executors.newFixedThreadPool(3);

    private final ExecutorService bagThreadPool = Executors.newFixedThreadPool(3);

    private final static ConcurrentMap callMap = new ConcurrentHashMap();

    public void init() {
        buildData();
        this.start();
    }

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

                String terminal = sendData.getTerminal();
                String unitId = sendData.getOwner();

                // 协议类型
                String terminalType = "";
                String protocolType = (String) data.get("protocolType");
                if ("trash-jt808".equals(protocolType)) {
                    terminalType = protocolJt808;
                }
                if ("trash-gb32960".equals(protocolType)) {
                    terminalType = protocolGb32960;
                }

                // 设备类型
                String trashType = (String) data.get("trashType");
                // 垃圾箱
                if ("trash-bin".equals(trashType)) {
                    CallInfo callInfo = fetchCallInfo("bin", unitId);
                    if (callInfo == null) {
                        log.warn("设备[{}]机构[{}]信息异常", terminal, unitId);
                        continue;
                    }

                    TrashSender trashSender = new TrashSender(tStarClient, trashProcess);
                    trashSender.setTerminalType(terminalType);
                    trashSender.setSendData(sendData);
                    trashSender.setCallInfo(callInfo);

                    trashThreadPool.execute(trashSender);
                    continue;
                }

                // 发放袋
                if ("trash-bag".equals(trashType)) {
                    CallInfo callInfo = fetchCallInfo("bag", unitId);
                    if (callInfo == null) {
                        log.warn("设备[{}]机构[{}]信息异常", terminal, unitId);
                        continue;
                    }

                    BagSender bagSender = new BagSender(tStarClient, bagProcess);
                    bagSender.setTerminalType(terminalType);
                    bagSender.setSendData(sendData);
                    bagSender.setBagOptProvider(bagOptProvider);
                    bagSender.setCallInfo(callInfo);

                    bagThreadPool.execute(bagSender);
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 加载接口数据
     */
    public void buildData() {
        // 加载接口数据
        try {
            String path = "conf/" + env + "/data.json";
            File file = new ClassPathResource(path).getFile();
            String data = FileUtils.readFileToString(file, "UTF-8");
            Map map = JacksonUtil.toObject(data, HashMap.class);
            callInfoProvider.put(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取设备接口数据
     *
     * @param type
     * @param key
     * @return
     */
    public CallInfo fetchCallInfo(String type, String key) {
        if (callMap.containsKey(key)) {

            return (CallInfo) callMap.get(key);
        }

        List<HashMap> list = (List) callInfoProvider.get(type);
        if (StringUtils.isEmpty(key) || CollectionUtils.isEmpty(list)) {

            return null;
        }

        CallInfo callInfo = null;
        try {
            for (int i = 0; i < list.size(); i++) {
                Map map = list.get(i);
                String id = (String) map.get("key");

                if (id.equals(key)) {
                    callInfo = new CallInfo();
                    BeanUtils.populate(callInfo, map);
                    callMap.put(key, callInfo);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return callInfo;
    }

    public void setConsumer(ConsumerConnector consumer) {
        this.consumer = consumer;
    }
}