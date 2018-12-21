package com.tiza.service.support.task;

import cn.com.tiza.tstar.datainterface.client.TStarSimpleClient;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.rp.support.model.HwHeader;
import com.tiza.rp.support.parse.HwDataProcess;
import com.tiza.service.support.task.abs.SendThread;

import java.util.Map;

/**
 * Description: TrashSender
 * Author: DIYILIU
 * Update: 2018-12-21 10:17
 */
public class TrashSender extends SendThread {

    public TrashSender(TStarSimpleClient tStarClient, HwDataProcess dataProcess) {
        this.tStarClient = tStarClient;
        this.dataProcess = dataProcess;
    }

    @Override
    public void run() {
        try {
            // 实时计算解析参数
            Map data = sendData.getData();
            // 透传指令ID
            int id = (int) data.get("id");

            byte[] bytes = CommonUtil.hexStringToBytes(sendData.getContent());
            HwHeader hwHeader = (HwHeader) dataProcess.parseHeader(bytes);

            int cmd = 0x8900;
            byte[] content = new byte[0];

            // 通用应答
            if (0x03 == id || 0x06 == id || 0x07 == id) {
                content = dataProcess.pack(hwHeader, new Object[0]);
            }
            // 用户信息查询
            if (0x04 == id) {
                Object[] objects = new Object[]{1, 12345678901l, "user", 10000};
                content = dataProcess.pack(hwHeader, objects);
            }
            // 投放数据
            if (0x05 == id) {
                Object[] objects = new Object[]{1, 100};
                content = dataProcess.pack(hwHeader, objects);
            }

            if (content.length > 1) {
                jt808Send(sendData.getTerminal(), cmd, CommonUtil.getMsgSerial(), content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
