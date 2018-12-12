package com.tiza.service.support;

import cn.com.tiza.tstar.datainterface.client.TStarSimpleClient;
import cn.com.tiza.tstar.datainterface.client.entity.ClientCmdSendResult;
import com.tiza.plugin.protocol.hw.HwDataProcess;
import com.tiza.plugin.protocol.hw.model.HwHeader;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description: SendConsumer
 * Author: DIYILIU
 * Update: 2018-12-11 14:12
 */

@Slf4j
public class SendConsumer extends Thread {

    private String sendTopic;

    private ConsumerConnector consumer;

    private String terminalType;

    @Resource
    private TStarSimpleClient tStarClient;

    @Resource
    private HwDataProcess hwDataProcess;

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
                Map data = JacksonUtil.toObject(text, HashMap.class);
                // 响应指令
                toSend(data);
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

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public void toSend(Map data) throws Exception {
        int type = hwDataProcess.getDataType();
        String terminal = (String) data.get("terminal");

        int cmd = 0x00;
        int serial = CommonUtil.getMsgSerial();
        byte[] content = null;
        // 垃圾箱
        if (type == 1001) {
            String str = (String) data.get("content");
            Map detail = (Map) data.get("data");
            int id = (int) detail.get("id");

            cmd = 0x8900;
            byte[] bytes = CommonUtil.hexStringToBytes(str);

            HwHeader hwHeader = (HwHeader) hwDataProcess.parseHeader(bytes);

            // 通用应答
            if (0x03 == id || 0x06 == id || 0x07 == id) {
                content = hwDataProcess.pack(hwHeader, new Object[0]);
            }

            // 用户信息查询
            if (0x04 == id) {
                Object[] objects = new Object[]{1, 12345678901l, "user", 10000};
                content = hwDataProcess.pack(hwHeader, objects);
            }
        }

        if (content == null || content.length < 1) {
            return;
        }

        // 生成下发指令
        byte[] bytes = CommonUtil.jt808Response(terminal, content, cmd, CommonUtil.getMsgSerial());
        log.info("指令下发内容[{}] ... ", CommonUtil.bytesToStr(bytes));

        // TStar 指令下发
        ClientCmdSendResult sendResult = tStarClient.cmdSend(terminalType, terminal, cmd, serial, bytes, 1);
        log.info("TSTAR 执行结果: [{}]", sendResult.getIsSuccess() ? "成功" : "失败");
    }
}
