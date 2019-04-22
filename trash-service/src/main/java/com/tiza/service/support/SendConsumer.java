package com.tiza.service.support;

import com.tiza.plugin.bean.VehicleInfo;
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
import org.springframework.beans.factory.InitializingBean;
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
public class SendConsumer extends Thread implements InitializingBean {
    private ConsumerConnector consumer;

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

    @Resource
    private ICache vehicleInfoProvider;

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

                String terminal = sendData.getTerminal();
                if (!vehicleInfoProvider.containsKey(terminal)) {
                    log.info("设备[{}]未注册!", terminal);
                    return;
                }

                VehicleInfo vehicleInfo = (VehicleInfo) vehicleInfoProvider.get(terminal);
                String unitId = vehicleInfo.getOwner();

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
                    List list = (List) callInfoProvider.get("bin");

                    TrashSender trashSender = new TrashSender(tStarClient, trashProcess);
                    trashSender.setTerminalType(terminalType);
                    trashSender.setSendData(sendData);
                    trashSender.setCallInfo(fetchCallInfo(list, unitId));

                    trashThreadPool.execute(trashSender);
                    continue;
                }

                // 发放袋
                if ("trash-bag".equals(trashType)) {
                    List list = (List) callInfoProvider.get("bag");

                    BagSender bagSender = new BagSender(tStarClient, bagProcess);
                    bagSender.setTerminalType(terminalType);
                    bagSender.setSendData(sendData);
                    bagSender.setBagOptProvider(bagOptProvider);
                    bagSender.setCallInfo(fetchCallInfo(list, unitId));

                    bagThreadPool.execute(bagSender);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public CallInfo fetchCallInfo(List list, String key) {
        CallInfo callInfo = new CallInfo();
        for (int i = 0; i < list.size(); i++) {
            Map map = (Map) list.get(i);
            String id = (String) map.get("key");
            try {
                if (id.equals(key)) {
                    BeanUtils.populate(callInfo, map);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return callInfo;
    }

    public void setConsumer(ConsumerConnector consumer) {
        this.consumer = consumer;
    }

    @Override
    public void afterPropertiesSet() {
        this.start();
    }
}
