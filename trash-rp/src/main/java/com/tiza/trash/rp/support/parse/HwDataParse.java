package com.tiza.trash.rp.support.parse;

import cn.com.tiza.tstar.common.process.BaseHandle;
import com.tiza.plugin.bean.VehicleInfo;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.model.DeviceData;
import com.tiza.plugin.model.Position;
import com.tiza.plugin.model.adapter.DataParseAdapter;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.trash.rp.support.model.HwHeader;
import com.tiza.trash.rp.support.model.KafkaMsg;
import com.tiza.trash.rp.support.model.SendData;
import com.tiza.trash.rp.support.parse.process.BagProcess;
import com.tiza.trash.rp.support.parse.process.TrashProcess;
import com.tiza.trash.rp.support.util.KafkaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.storm.guava.collect.Lists;
import org.apache.storm.guava.collect.Maps;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

/**
 * Description: HwDataParse
 * Author: DIYILIU
 * Update: 2018-12-19 10:15
 */

@Slf4j
@Service
public class HwDataParse extends DataParseAdapter {
    private final List<String> canIds = Lists.newArrayList("AA");

    @Value("${tstar.track.topic}")
    private String trackTopic;

    @Value("${tstar.work.topic}")
    private String workTopic;

    @Resource
    private ICache vehicleInfoProvider;

    @Resource
    private TrashProcess trashProcess;

    @Resource
    private BagProcess bagProcess;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Value("${kafka.send-topic}")
    private String sendTopic;

    @Value("${trash.bin}")
    private Integer binCode;

    @Value("${trash.bag}")
    private Integer bagCode;

    @Value("${protocol.jt808}")
    private String jt808Code;

    @Value("${protocol.gb32960}")
    private String gb32960Code;

    @Override
    public void detach(DeviceData deviceData) {
        String terminalId = deviceData.getDeviceId();
        byte[] bytes = deviceData.getBytes();
        if (bytes == null || bytes.length < 1) {
            return;
        }
        long gwTime = deviceData.getTime();
        int cmd = deviceData.getCmdId();

        VehicleInfo vehicleInfo = (VehicleInfo) vehicleInfoProvider.get(terminalId);
        int vehType = vehicleInfo.getVehType();
        String protocol = vehicleInfo.getProtocol();

        String trashType = "";
        HwDataProcess hwDataProcess = null;

        // 设备类型
        if (vehType == binCode) {
            trashType = "trash-bin";
            hwDataProcess = trashProcess;
        }
        if (vehType == bagCode) {
            trashType = "trash-bag";
            hwDataProcess = bagProcess;
        }

        // 应答指令ID
        int respCmd = 0;
        // 协议类型
        String protocolType = "";
        if (jt808Code.equals(protocol)) {
            protocolType = "trash-jt808";
            respCmd = 0x8900;
        }
        if (gb32960Code.equals(protocol)) {
            protocolType = "trash-gb32960";
            respCmd = cmd;
        }
        if (hwDataProcess == null) {
            log.info("设备类型异常: [{}]", vehType);
            return;
        }

        try {
            HwHeader hwHeader = (HwHeader) hwDataProcess.parseHeader(bytes);
            if (hwHeader != null) {
                hwHeader.setTerminalId(terminalId);
                // 设置网关时间
                hwHeader.setTime(gwTime);

                hwDataProcess.parse(hwHeader.getContent(), hwHeader);
                Map headerMap = hwHeader.getParamMap();

                log.info("设备[{}]透传工况数据[{}]", terminalId, JacksonUtil.toJson(headerMap));
                if (MapUtils.isNotEmpty(headerMap)) {
                    Map param = new HashMap();
                    param.put("id", hwHeader.getCmd());
                    param.put("trashType", trashType);
                    param.put("protocolType", protocolType);
                    param.putAll(headerMap);

                    // 构造数据
                    SendData sendData = new SendData();
                    sendData.setOwner(vehicleInfo.getOwner());
                    sendData.setTerminal(terminalId);
                    sendData.setCmd(cmd);
                    sendData.setRespCmd(respCmd);
                    sendData.setContent(CommonUtil.bytesToStr(bytes));
                    sendData.setData(param);
                    sendData.setTime(System.currentTimeMillis());

                    // 写入 kafka
                    sendToKafka(sendData);
                    // 更新工况表
                    updateWorkInfo(terminalId, headerMap);
                    // 写入kafka
                    Map map = Maps.newHashMap();
                    map.put("time", new Date(hwHeader.getTime()));
                    map.put("type", hwHeader.getCmd());
                    map.put("data", headerMap);
                    sendToTStar(String.valueOf(vehicleInfo.getId()), deviceData.getCmdId(), JacksonUtil.toJson(map), deviceData.getTime(), workTopic);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dealWithTStar(DeviceData deviceData, BaseHandle handle) {
        // gb32960 0x02
        if (deviceData.getDataBody() != null) {
            List<Map> paramValues = (List<Map>) deviceData.getDataBody();

            for (int i = 0; i < paramValues.size(); i++) {
                Map map = paramValues.get(i);
                for (Iterator iterator = map.keySet().iterator(); iterator.hasNext(); ) {
                    String key = (String) iterator.next();
                    Object value = map.get(key);

                    // 位置信息
                    if (key.equalsIgnoreCase("position")) {
                        Position position = (Position) value;
                        position.setTime(deviceData.getTime());
                        position.setCmd(deviceData.getCmdId());
                        dealPosition(deviceData.getDeviceId(), position);
                    }
                    // 透传数据
                    if (canIds.contains(key)) {
                        byte[] bytes = (byte[]) value;
                        deviceData.setBytes(bytes);
                        deviceData.setDataType(key);

                        detach(deviceData);
                    }
                }
            }
        }
    }

    public void sendToKafka(SendData sendData) {
        String terminal = sendData.getTerminal();
        String json = JacksonUtil.toJson(sendData);

        KafkaUtil.send(new KafkaMsg(terminal, json, sendTopic));
        log.info("终端[{}]写入 kafka [{}]", terminal, json);
    }

    public void sendToTStar(String device, int cmd, String value, long time, String topic) {
        byte[] bytes = CommonUtil.tstarKafkaArray(device, cmd, value, time, 0);
        KafkaMsg msg = new KafkaMsg(device, bytes, topic);
        msg.setTstar(true);
        KafkaUtil.send(msg);
    }

    @Override
    public void sendToDb(String sql, Object... args) {

        jdbcTemplate.update(sql, args);
    }

    @Override
    public void dealPosition(String deviceId, Position position) {
        VehicleInfo vehicleInfo = (VehicleInfo) vehicleInfoProvider.get(deviceId);

        Object[] args = new Object[]{position.getLng(), position.getLat(), position.getEnLng(), position.getEnLat(), position.getProvince(), position.getCity(), position.getArea(),
                new Date(position.getTime()), new Date(), vehicleInfo.getId()};

        String sql = "UPDATE serv_device_position " +
                "SET " +
                " longitude = ?," +
                " latitude = ?," +
                " encrypt_long = ?," +
                " encrypt_lat = ?," +
                " province = ?," +
                " city = ?, " +
                " area = ?, " +
                " gps_time= ?," +
                " modified_date = ?" +
                "WHERE " +
                "device_id = ?";

        sendToDb(sql, args);
    }

    /**
     * 更新当前位置信息
     *
     * @param terminal
     * @param map
     */
    public void updateWorkInfo(String terminal, Map map) {
        VehicleInfo vehicleInfo = (VehicleInfo) vehicleInfoProvider.get(terminal);

        String sql = "SELECT t.data_json  FROM serv_device_work t WHERE t.device_id=" + vehicleInfo.getId();
        String json = jdbcTemplate.queryForObject(sql, String.class);

        // 工况参数
        Map workMap = new HashMap();
        try {
            if (StringUtils.isNotEmpty(json)) {
                workMap = JacksonUtil.toObject(json, HashMap.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 覆盖历史工况信息
        workMap.putAll(map);

        json = JacksonUtil.toJson(workMap);
        Object[] args = new Object[]{json, new Date(), vehicleInfo.getId()};
        sql = "UPDATE serv_device_work " +
                "SET " +
                " data_json = ?, " +
                " modified_date = ? " +
                "WHERE " +
                "device_id = ?";

        sendToDb(sql, args);
    }
}
