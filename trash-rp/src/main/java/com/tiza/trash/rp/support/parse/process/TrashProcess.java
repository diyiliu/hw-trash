package com.tiza.trash.rp.support.parse.process;

import com.tiza.plugin.model.Header;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.trash.rp.support.model.HwHeader;
import com.tiza.trash.rp.support.parse.HwDataProcess;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description: TrashProcess
 * Author: DIYILIU
 * Update: 2018-12-10 14:19
 */

@Slf4j
@Service
public class TrashProcess extends HwDataProcess {

    @Override
    public Header parseHeader(byte[] bytes) {
        if (bytes.length < 10) {
            log.info("透传数据长度异常; [{}]", CommonUtil.bytesToStr(bytes));

            return null;
        }

        HwHeader header = null;
        try {
            ByteBuf buf = Unpooled.copiedBuffer(bytes);
            // 0xF1
            buf.readByte();
            // 0xFE,0xFE
            buf.readBytes(new byte[2]);

            byte[] startBytes = new byte[3];
            buf.readBytes(startBytes);

            int cmd = buf.readByte();
            int length = buf.readByte();
            if (buf.readableBytes() < length + 2) {
                log.warn("数据长度不足: [{}]", CommonUtil.bytesToStr(bytes));
                return null;
            }

            byte[] content = new byte[length];
            buf.readBytes(content);
            // 暂时不验证校验位
            byte check = buf.readByte();
            byte endByte = buf.readByte();

            header = new HwHeader();
            header.setStartBytes(startBytes);
            header.setCmd(cmd);
            header.setContent(content);
            header.setEndByte(endByte);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return header;
    }

    @Override
    public void parse(byte[] content, Header header) {
        HwHeader hwHeader = (HwHeader) header;

        Map param = new HashMap();
        int cmd = hwHeader.getCmd();
        ByteBuf buf = Unpooled.copiedBuffer(content);
        // 设备状态上传
        if (0x03 == cmd) {
            int tempFlag = buf.readByte();
            double temp = CommonUtil.keepDecimal(buf.readShort(), 0.1, 1);
            if (tempFlag == 1) {
                temp *= -1;
            }

            int n = buf.readByte();
            List list = new ArrayList();
            for (int i = 0; i < n; i++) {
                int category = buf.readByte();
                int distance = buf.readUnsignedByte();

                /*
                Map map = new HashMap();
                map.put("category", category);
                map.put("distance", distance);
                list.add(map);
                */
                list.add(distance);
            }

            param.put("temperature", temp);
            param.put("binsRange", list);

            /*
            Object[] args = new Object[]{terminalId, JacksonUtil.toJson(param), gwTime};
            String sql = "INSERT INTO trashcan_work_param_log(ter_no, work_param, gw_time) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, args);
            return;
            */
        }
        // 用户信息查询
        else if (0x04 == cmd) {
            int authType = buf.readByte();

            int length = buf.readUnsignedByte();
            byte[] array = new byte[length];
            buf.readBytes(array);

            String authContent = new String(array);

            param.put("authType", authType);
            param.put("authContent", authContent);

             /*
            Object[] args = new Object[]{terminalId, authType, authContent, gwTime};
            String sql = "INSERT INTO trashcan_card_log(ter_no, auth_type, auth_content, gw_time) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, args);
            return;
            */
        }
        else if (0x05 == cmd) {
            int authType = buf.readByte();
            int length = buf.readUnsignedByte();

            byte[] array = new byte[length];
            buf.readBytes(array);
            String authContent = new String(array);

            int n = buf.readByte();
            if (buf.readableBytes() < n * 6) {

                log.error("数据长度不足: [{}]", CommonUtil.bytesToStr(content));
                return;
            }
            List list = new ArrayList();
            for (int i = 0; i < n; i++) {

                int category = buf.readByte();
                int weight = buf.readShort();
                int before = buf.readShort();
                int after = buf.readShort();

                Map wMap = new HashMap();
                wMap.put("category", category);
                wMap.put("weight", CommonUtil.keepDecimal(weight, 0.01, 2));
                wMap.put("before", CommonUtil.keepDecimal(before, 0.01, 2));
                wMap.put("after", CommonUtil.keepDecimal(after, 0.01, 2));
                list.add(wMap);
            }

            param.put("authType", authType);
            param.put("authContent", authContent);
            param.put("binsWeight", list);

            /*
            Object[] args = new Object[]{terminalId, authType, authContent, n, JacksonUtil.toJson(list), gwTime};
            String sql = "INSERT INTO trashcan_card_pick_log(ter_no, auth_type, auth_content, channel_count, pick_content, gw_time) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, args);
            return;
            */
        }
        else if (0x06 == cmd) {
            int n = buf.readByte();
            if (buf.readableBytes() < n) {
                log.error("数据长度不足: [{}]", CommonUtil.bytesToStr(content));
                return;
            }

            List list = new ArrayList();
            for (int i = 0; i < n; i++) {
                int category = buf.readByte();
                int fault = buf.readByte();

               /*
                Map map = new HashMap();
                map.put("category", category);
                map.put("fault", fault);
                list.add(map);
                */
                list.add(fault);
            }
            param.put("binsFault", list);

            /*
            Object[] args = new Object[]{terminalId, n, JacksonUtil.toJson(list), gwTime};
            String sql = "INSERT INTO trashcan_fault_log(ter_no, channel_count, fault_content, gw_time) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, args);
            return;
            */
        }
        // 清理签到上传
        else if (0x07 == cmd) {
            int authType = buf.readByte();

            int length = buf.readUnsignedByte();
            byte[] array = new byte[length];
            buf.readBytes(array);

            String authContent = new String(array);
            param.put("authType", authType);
            param.put("authContent", authContent);
        }
        // XJ6000 设备状态信息上传
        else if (0x3C == cmd) {
            double hDeg = CommonUtil.keepDecimal(buf.readUnsignedShort() - 10000, 0.1, 1);
            double vDeg = CommonUtil.keepDecimal(buf.readUnsignedShort() - 10000, 0.1, 1);

            // 横轴角度
            param.put("hDeg", hDeg);
            // 纵轴角度
            param.put("vDeg", vDeg);
            // 垃圾满溢标识
            param.put("overflow", buf.readByte());

            /*
            Object[] args = new Object[]{terminalId, JacksonUtil.toJson(param), gwTime};
            String sql = "INSERT INTO trashcan_work_param_log(ter_no, work_param, gw_time) VALUES (?,  ?, ?)";
            jdbcTemplate.update(sql, args);
            return;
            */
        }

        if (MapUtils.isNotEmpty(param)){
            param.put("gpsTime", hwHeader.getTime());
            param.put("type", cmd);

            hwHeader.setParamMap(param);
        }
    }

    @Override
    public byte[] pack(Header header, Object... argus) {
        HwHeader hwHeader = (HwHeader) header;
        int cmd = hwHeader.getCmd();

        byte[] startBytes = hwHeader.getStartBytes();

        if (0x03 == cmd || 0x06 == cmd || 0x07 == cmd || 0x3C == cmd) {
            ByteBuf buf = Unpooled.copiedBuffer(startBytes, new byte[]{(byte) cmd, 0x01, 0x01});
            return combine(buf.array());
        }

        if (0x04 == cmd) {
            int status = (int) argus[0];
            String user = (String) argus[1];
            String account = (String) argus[2];
            int money = (int) argus[3];

            byte[] userBytes = user.getBytes(Charset.forName("UTF-8"));
            int userLen = userBytes.length;
            byte[] userArray = new byte[28];
            System.arraycopy(userBytes, 0, userArray, 0, userLen);

            byte[] accountBytes = account.getBytes();
            int accountLen = accountBytes.length;

            ByteBuf buf = Unpooled.buffer(41 + accountLen);
            buf.writeBytes(startBytes);
            buf.writeByte(cmd);
            buf.writeByte(36 + accountLen);
            buf.writeByte(status);
            buf.writeByte(userLen);
            buf.writeBytes(userArray);
            buf.writeInt(money);
            buf.writeByte(0);
            buf.writeByte(accountLen);
            buf.writeBytes(accountBytes);

            return combine(buf.array());
        }

        if (0x05 == cmd) {
            int status = (int) argus[0];
            int money = (int) argus[1];

            ByteBuf buf = Unpooled.buffer(10);
            buf.writeBytes(startBytes);
            buf.writeByte(cmd);
            buf.writeByte(5);
            buf.writeByte(status);
            buf.writeInt(money);

            return combine(buf.array());
        }

        return null;
    }

    public byte[] combine(byte[] content) {
        byte check = CommonUtil.sumCheck(content);

        ByteBuf buf = Unpooled.buffer(content.length + 5);
        buf.writeByte(0xF1);
        buf.writeByte(0xFE);
        buf.writeByte(0xFE);
        buf.writeBytes(content);
        buf.writeByte(check);
        buf.writeByte(0x16);

        return buf.array();
    }
}
