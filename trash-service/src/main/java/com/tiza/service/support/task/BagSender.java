package com.tiza.service.support.task;

import cn.com.tiza.tstar.datainterface.client.TStarSimpleClient;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.HttpUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.rp.support.model.HwHeader;
import com.tiza.rp.support.parse.HwDataProcess;
import com.tiza.service.support.model.CardInfo;
import com.tiza.service.support.task.abs.SendThread;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description: BagSender
 * Author: DIYILIU
 * Update: 2018-12-21 10:00
 */

@Slf4j
public class BagSender extends SendThread {

    private ICache bagOptProvider;

    private String ticket;

    public BagSender(TStarSimpleClient tStarClient, HwDataProcess dataProcess) {
        this.tStarClient = tStarClient;
        this.dataProcess = dataProcess;
    }

    @Override
    public void run() {
        // 实时计算解析参数
        Map data = sendData.getData();
        // 透传指令ID
        int id = (int) data.get("id");

        byte[] bytes = CommonUtil.hexStringToBytes(sendData.getContent());
        HwHeader hwHeader = (HwHeader) dataProcess.parseHeader(bytes);

        int cmd = 0x8900;
        int readWrite = hwHeader.getReadWrite();

        String terminal = sendData.getTerminal();
        try {
            if (0x20 == readWrite) {
                Map param = new HashMap();
                int status = 2;

                CardInfo cardInfo;
                String json;
                Map map;
                int errcode;
                switch (id) {
                    case 0x01:
                        String userId = (String) data.get("userId");
                        param.put("action", "cardinfo");
                        param.put("ticket", ticket);
                        param.put("devno", terminal);
                        param.put("idcard", userId);

                        json = HttpUtil.getForString(baseUrl, param);
                        map = JacksonUtil.toObject(json, HashMap.class);
                        errcode = (int) map.get("errcode");
                        if (errcode == 0) {
                            cardInfo = JacksonUtil.toObject(json, CardInfo.class);
                            cardInfo.setCardId(userId);
                            bagOptProvider.put("user:" + terminal, cardInfo);
                            status = 1;
                        }

                        break;
                    case 0x02:
                        cardInfo = (CardInfo) bagOptProvider.get("user:" + terminal);
                        if (cardInfo == null) {
                            log.error("CardInfo 信息丢失, 无法进行鉴权积分卡操作!");
                            return;
                        }
                        String password = (String) data.get("password");
                        param.put("action", "authcard");
                        param.put("ticket", ticket);
                        param.put("devno", terminal);
                        param.put("idcard", cardInfo.getCardId());
                        param.put("pwd", password);

                        json = HttpUtil.getForString(baseUrl, param);
                        map = JacksonUtil.toObject(json, HashMap.class);
                        errcode = (int) map.get("errcode");
                        if (errcode == 0) {
                            String token = (String) map.get("token");
                            cardInfo.setToken(token);
                            status = 1;
                        }

                        break;
                    case 0x03:
                        String workerId = (String) data.get("workerId");
                        String workerPw = (String) data.get("workerPw");
                        param.put("action", "login");
                        param.put("ticket", ticket);
                        param.put("devno", terminal);
                        param.put("account", workerId);
                        param.put("pwd", workerPw);

                        json = HttpUtil.getForString(baseUrl, param);
                        map = JacksonUtil.toObject(json, HashMap.class);
                        errcode = (int) map.get("errcode");
                        if (errcode == 0) {
                            String token = (String) map.get("token");
                            bagOptProvider.put("worker:" + terminal, token);
                            status = 1;
                        }

                        break;
                    case 0x04:
                        cardInfo = (CardInfo) bagOptProvider.get("user:" + terminal);
                        if (cardInfo == null) {
                            log.error("CardInfo 信息丢失, 无法进行鉴权积分卡操作!");
                            return;
                        }

                        String bagPush = (String) data.get("bagPush");
                        param.put("action", "submit");
                        param.put("ticket", ticket);
                        param.put("devno", terminal);
                        param.put("idcard", cardInfo.getCardId());
                        param.put("token", cardInfo.getToken());
                        param.put("data", bagPush);

                        json = HttpUtil.getForString(baseUrl, param);
                        map = JacksonUtil.toObject(json, HashMap.class);
                        errcode = (int) map.get("errcode");
                        if (errcode == 0) {
                            status = 1;
                        }

                        break;
                    case 0x05:
                        String workerToken = (String) bagOptProvider.get("worker:" + terminal);
                        String bagPut = (String) data.get("bagPut");

                        param.put("action", "replen");
                        param.put("ticket", ticket);
                        param.put("devno", terminal);
                        param.put("token", workerToken);
                        param.put("x", bagPut);

                        json = HttpUtil.getForString(baseUrl, param);
                        map = JacksonUtil.toObject(json, HashMap.class);
                        errcode = (int) map.get("errcode");
                        if (errcode == 0) {
                            status = 1;
                        }

                        break;
                    default:
                        log.warn("功能编号[{}]异常!", cmd);
                }

                byte[] content = dataProcess.pack(hwHeader, status);
                if (content != null && content.length > 1) {
                    jt808Send(sendData.getTerminal(), cmd, CommonUtil.getMsgSerial(), content);
                }

                return;
            }

            if (0x40 == readWrite) {
                byte[] content = new byte[0];

                CardInfo cardInfo;
                switch (id) {
                    case 0x01:
                        content = dataProcess.pack(hwHeader, null);

                        break;
                    case 0x02:
                        cardInfo = (CardInfo) bagOptProvider.get("user:" + terminal);
                        if (cardInfo == null) {
                            log.error("CardInfo 信息丢失, 无法进行读操作0x02!");
                            return;
                        }
                        Object[] args = new Object[]{cardInfo.getPhone(), cardInfo.getBalance(), cardInfo.getName()};
                        content = dataProcess.pack(hwHeader, args);

                        break;
                    case 0x03:
                        cardInfo = (CardInfo) bagOptProvider.get("user:" + terminal);
                        if (cardInfo == null) {
                            log.error("CardInfo 信息丢失, 无法进行读操作0x03!");
                            return;
                        }

                        Map param = new HashMap();
                        param.put("action", "product");
                        param.put("ticket", ticket);
                        param.put("devno", terminal);
                        param.put("token", cardInfo.getToken());
                        param.put("idcard", cardInfo.getCardId());

                        String json = HttpUtil.getForString(baseUrl, param);
                        Map map = JacksonUtil.toObject(json, HashMap.class);
                        int errcode = (int) map.get("errcode");
                        if (errcode == 0) {
                            List dataList = (List) map.get("data");
                            content = dataProcess.pack(hwHeader, dataList);
                        }else {
                            content = new byte[]{0};
                        }
                        break;
                    default:
                        break;
                }

                if (content != null && content.length > 1) {
                    jt808Send(sendData.getTerminal(), cmd, CommonUtil.getMsgSerial(), content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setBagOptProvider(ICache bagOptProvider) {
        this.bagOptProvider = bagOptProvider;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
}
