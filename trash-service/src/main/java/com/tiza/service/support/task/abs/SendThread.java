package com.tiza.service.support.task.abs;

import cn.com.tiza.tstar.datainterface.client.TStarSimpleClient;
import cn.com.tiza.tstar.datainterface.client.entity.ClientCmdSendResult;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.rp.support.model.SendData;
import com.tiza.rp.support.parse.HwDataProcess;
import lombok.extern.slf4j.Slf4j;

/**
 * Description: SendThread
 * Author: DIYILIU
 * Update: 2018-12-21 09:56
 */

@Slf4j
public abstract class SendThread implements Runnable {

    // tstar 客户端
    protected TStarSimpleClient tStarClient;
    // tstar 设备类型
    protected String terminalType;

    // 透传数据
    protected SendData sendData;
    // 处理类
    protected HwDataProcess dataProcess;
    // 请求路径
    protected String baseUrl;

    public void jt808Send(String terminal, int cmd, int serial, byte[] content) throws Exception {
        // 生成下发指令
        byte[] bytes = CommonUtil.jt808Response(terminal, content, cmd, serial);
        log.info("指令下发内容[{}] ... ", CommonUtil.bytesToStr(bytes));

        // TStar 指令下发
        ClientCmdSendResult sendResult = tStarClient.cmdSend(terminalType, terminal, cmd, serial, bytes, 1);
        log.info("TSTAR 执行结果: [{}]", sendResult.getIsSuccess() ? "成功" : "失败");
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public void setData(SendData sendData) {
        this.sendData = sendData;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
