package com.tiza.service.support.task;

import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.HttpUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.rp.support.model.HwHeader;
import com.tiza.rp.support.parse.HwDataProcess;
import com.tiza.service.support.client.TStarClientAdapter;
import com.tiza.service.support.task.abs.SendThread;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description: TrashSender
 * Author: DIYILIU
 * Update: 2018-12-21 10:17
 */

@Slf4j
public class TrashSender extends SendThread {

    public TrashSender(TStarClientAdapter tStarClient, HwDataProcess dataProcess) {
        this.tStarClient = tStarClient;
        this.dataProcess = dataProcess;
    }

    @Override
    public void run() {
        try {
            String token = callInfo.fetchToken();
            if (StringUtils.isEmpty(token)) {
                log.info("票据获取失败!");
                return;
            }
            String baseUrl = callInfo.getUrl();

            // 实时计算解析参数
            Map data = sendData.getData();
            String terminal = sendData.getTerminal();
            // 透传指令ID
            int id = (int) data.get("id");

            byte[] bytes = CommonUtil.hexStringToBytes(sendData.getContent());
            HwHeader hwHeader = (HwHeader) dataProcess.parseHeader(bytes);
            hwHeader.setTerminalId(terminal);

            // 应答指令ID
            int cmd = sendData.getRespCmd();

            byte[] content = new byte[0];
            Map param = new HashMap();
            String json;
            Map map;
            int errcode;

            String reqDevice = terminal;
            if (callInfo.getName().contains("中航")) {
                reqDevice += "7511837323734";
            }

            // 设备状态信息上传
            if (0x03 == id) {
                content = dataProcess.pack(hwHeader, new Object[0]);

                // 特殊处理(设备状态跟着0x02指令透传)
                if (0x03 == id && terminalType.contains("gb32960")) {
                    int length = content.length;
                    ByteBuf buf = Unpooled.buffer(9 + length);
                    buf.writeBytes(CommonUtil.dateToBytes(new Date()));
                    buf.writeByte(0xAA);
                    buf.writeShort(length);
                    buf.writeBytes(content);

                    content = buf.array();
                }

                double temperature = (double) data.get("temperature");
                List<Integer> list = (List) data.get("binsRange");
                String capacities = "";
                if (CollectionUtils.isNotEmpty(list)) {
                    for (Integer i : list) {
                        capacities += i + ",";
                    }
                }

                param.put("token", token);
                param.put("device", reqDevice);
                param.put("temperature", temperature);
                param.put("capacities", capacities.substring(0, capacities.length() - 1));

                json = HttpUtil.postForString(baseUrl + "/status", param);
                map = JacksonUtil.toObject(json, HashMap.class);
                errcode = (int) map.get("errcode");
                if (errcode == 0) {
                    log.info("设备[{}]状态信息推送成功!", terminal);
                } else {
                    callInfo.setExpire(0);
                    log.info("设备[{}]状态信息推送失败: {}", terminal, map.get("errmsg"));
                }
            }
            // 用户信息查询
            else if (0x04 == id) {
                int authType = (int) data.get("authType");
                String authContent = (String) data.get("authContent");
                param.put("token", token);
                param.put("type", authType);
                param.put("auth", authContent);
                param.put("device", reqDevice);

                json = HttpUtil.getForString(baseUrl + "/userinfo", param);
                map = JacksonUtil.toObject(json, HashMap.class);
                errcode = (int) map.get("errcode");
                if (errcode == 0) {
                    String user = (String) map.get("name");
                    String account = (String) map.get("account");
                    int money = (int) map.get("money");

                    Object[] objects = new Object[]{1, user, account, money};
                    content = dataProcess.pack(hwHeader, objects);
                } else {
                    callInfo.setExpire(0);
                    log.info("设备[{}]用户信息查询失败: {}", terminal, map.get("errmsg"));
                }
            }
            // 投放数据
            else if (0x05 == id) {
                int authType = (int) data.get("authType");
                String authContent = (String) data.get("authContent");
                param.put("token", token);
                param.put("device", reqDevice);

                Map jsonMap = new HashMap();
                jsonMap.put("type", authType);
                jsonMap.put("auth", authContent);
                jsonMap.put("data", data.get("binsWeight"));

                json = HttpUtil.postWithJsonAndParameter(baseUrl + "/throwin", JacksonUtil.toJson(jsonMap), param);
                map = JacksonUtil.toObject(json, HashMap.class);
                errcode = (int) map.get("errcode");

                int status = 0;
                int money = 0;
                if (errcode == 0) {
                    status = 1;
                    if (map.containsKey("money")) {
                        money = (int) map.get("money");
                    }
                } else {
                    callInfo.setExpire(0);
                    log.info("设备[{}]投放数据推送失败: {}", terminal, map.get("errmsg"));
                }

                Object[] objects = new Object[]{status, money};
                content = dataProcess.pack(hwHeader, objects);
            }
            // 故障码上传
            else if (0x06 == id) {
                content = dataProcess.pack(hwHeader, new Object[0]);

                param.put("token", token);
                param.put("device", reqDevice);

                List list = (List) data.get("binsFault");
                json = HttpUtil.postWithJsonAndParameter(baseUrl + "/throwin", JacksonUtil.toJson(list), param);
                map = JacksonUtil.toObject(json, HashMap.class);
                errcode = (int) map.get("errcode");
                if (errcode == 0) {
                    log.info("设备[{}]故障码数据推送成功!", terminal);
                } else {
                    callInfo.setExpire(0);
                    log.info("设备[{}]故障码数据推送失败: {}", terminal, map.get("errmsg"));
                }
            }
            // 清理签到
            else if (0x07 == id) {
                content = dataProcess.pack(hwHeader, new Object[0]);

                int authType = (int) data.get("authType");
                String authContent = (String) data.get("authContent");
                param.put("token", token);
                param.put("type", authType);
                param.put("auth", authContent);
                param.put("device", reqDevice);

                json = HttpUtil.getForString(baseUrl + "/clean", param);
                map = JacksonUtil.toObject(json, HashMap.class);
                errcode = (int) map.get("errcode");
                if (errcode == 0) {
                    log.info("设备[{}]清理签到数据推送成功!", terminal);
                } else {
                    callInfo.setExpire(0);
                }
            }

            if (content != null && content.length > 1) {
                toSend(sendData.getTerminal(), cmd, CommonUtil.getMsgSerial(), content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }
}
