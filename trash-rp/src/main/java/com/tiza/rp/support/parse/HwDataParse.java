package com.tiza.rp.support.parse;

import com.tiza.plugin.bean.VehicleInfo;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.model.DeviceData;
import com.tiza.plugin.model.Position;
import com.tiza.plugin.model.adapter.DataParseAdapter;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.rp.support.model.HwHeader;
import com.tiza.rp.support.model.SendData;
import com.tiza.rp.support.parse.process.BagProcess;
import com.tiza.rp.support.parse.process.TrashProcess;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Description: HwDataParse
 * Author: DIYILIU
 * Update: 2018-12-19 10:15
 */

@Slf4j
@Service
public class HwDataParse extends DataParseAdapter {

    @Resource
    private ICache vehicleInfoProvider;

    @Resource
    private TrashProcess trashProcess;

    @Resource
    private BagProcess bagProcess;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private Producer kafkaProducer;

    @Value("${kafka.send-topic}")
    private String sendTopic;

    @Value("${trash.bin}")
    private Integer binCode;

    @Value("${trash.bag}")
    private Integer bagCode;

    @Override
    public void detach(DeviceData deviceData) {
        String terminalId = deviceData.getDeviceId();
        byte[] bytes = deviceData.getBytes();
        long gwTime = deviceData.getTime();

        VehicleInfo vehicleInfo = (VehicleInfo) vehicleInfoProvider.get(terminalId);
        int vehType = vehicleInfo.getVehType();

        String trashType = "";
        HwDataProcess hwDataProcess = null;

        // 智能垃圾箱
        if (vehType == binCode) {
            trashType = "trash-bin";
            hwDataProcess = trashProcess;
        }

        // 垃圾袋发放机
        if (vehType == bagCode) {
            trashType = "trash-bag";
            hwDataProcess = bagProcess;
        }

        if (hwDataProcess == null) {
            log.info("设备类型异常: [{}]", vehType);
            return;
        }

        HwHeader hwHeader = (HwHeader) hwDataProcess.parseHeader(bytes);
        if (hwHeader != null) {
            hwHeader.setTerminalId(terminalId);
            // 设置网关时间
            hwHeader.setTime(gwTime);

            hwDataProcess.parse(hwHeader.getContent(), hwHeader);
            Map headerMap = hwHeader.getParamMap();
            if (headerMap != null) {
                Map param = new HashMap();
                param.put("id", hwHeader.getCmd());
                param.put("trashType", trashType);
                param.putAll(headerMap);

                // 构造数据
                int cmd = deviceData.getCmdId();
                SendData sendData = new SendData();
                sendData.setTerminal(terminalId);
                sendData.setCmd(cmd);
                sendData.setContent(CommonUtil.bytesToStr(bytes));
                sendData.setData(param);
                sendData.setTime(System.currentTimeMillis());
                // 写入 kafka
                sendToKafka(sendData);

                // 更新工况表
                updateWorkInfo(hwHeader);
            }
        }
    }

    public void sendToKafka(SendData sendData) {
        String terminal = sendData.getTerminal();

        String json = JacksonUtil.toJson(sendData);
        kafkaProducer.send(new KeyedMessage(sendTopic, terminal, json));
        log.info("终端[{}]写入 kafka [{}]", terminal, json);
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
     * @param hwHeader
     */
    public void updateWorkInfo(HwHeader hwHeader) {
        VehicleInfo vehicleInfo = (VehicleInfo) vehicleInfoProvider.get(hwHeader.getTerminalId());

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
        workMap.putAll(hwHeader.getParamMap());

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
