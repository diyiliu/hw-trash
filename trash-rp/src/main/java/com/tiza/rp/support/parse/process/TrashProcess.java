package com.tiza.rp.support.parse.process;

import com.tiza.plugin.model.Header;
import com.tiza.rp.support.parse.HwDataProcess;
import com.tiza.rp.support.model.HwHeader;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.*;

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
            log.error("数据长度不足: [{}]", CommonUtil.bytesToStr(bytes));
            return null;
        }

        byte[] content = new byte[length];
        buf.readBytes(content);
        // 暂时不验证校验位
        byte check = buf.readByte();
        byte endByte = buf.readByte();

        HwHeader header = new HwHeader();
        header.setStartBytes(startBytes);
        header.setCmd(cmd);
        header.setContent(content);
        header.setEndByte(endByte);

        return header;
    }

    @Override
    public void parse(byte[] content, Header header) {
        HwHeader hwHeader = (HwHeader) header;
        String terminalId = hwHeader.getTerminalId();
        Date gwTime = new Date(hwHeader.getTime());

        Map param = new HashMap();
        int cmd = hwHeader.getCmd();
        ByteBuf buf = Unpooled.copiedBuffer(content);

        if (0x03 == cmd) {
            int tempFlag = buf.readByte();
            double temp = CommonUtil.keepDecimal(buf.readShort(), 0.1, 1);
            if (tempFlag == 1) {
                temp *= -1;
            }

            int n = buf.readByte();
            if (buf.readableBytes() < n) {

                log.error("数据长度不足: [{}]", CommonUtil.bytesToStr(content));
            }
            int[] array = new int[n];
            for (int i = 0; i < n; i++) {
                array[i] = buf.readByte();
            }

            param.put("temperature", temp);
            param.put("binsRange", array);
            hwHeader.setParamMap(param);

            Object[] args = new Object[]{terminalId, temp, JacksonUtil.toJson(array), gwTime};
            String sql = "INSERT INTO veh_work_param_log (ter_no, temperature, work_param, gw_time) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, args);

            return;
        }

        if (0x04 == cmd) {
            int authType = buf.readByte();
            int length = buf.readUnsignedByte();
            if (buf.readableBytes() < length) {

                log.error("数据长度不足: [{}]", CommonUtil.bytesToStr(content));
            }

            byte[] array = new byte[length];
            buf.readBytes(array);
            String authContent = new String(array);

            param.put("authType", authType);
            param.put("authContent", authContent);
            hwHeader.setParamMap(param);

            Object[] args = new Object[]{terminalId, authType, authContent, gwTime};
            String sql = "INSERT INTO veh_card_log (ter_no, auth_type, auth_content, gw_time) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, args);

            return;
        }

        if (0x05 == cmd) {
            int authType = buf.readByte();
            int length = buf.readUnsignedByte();
            if (buf.readableBytes() < length) {

                log.error("数据长度不足: [{}]", CommonUtil.bytesToStr(content));
            }

            byte[] array = new byte[length];
            buf.readBytes(array);
            String authContent = new String(array);

            int n = buf.readByte();
            if (buf.readableBytes() < n * 6) {

                log.error("数据长度不足: [{}]", CommonUtil.bytesToStr(content));
            }

            List list = new ArrayList();
            for (int i = 0; i < n; i++) {

                int weight = buf.readShort();
                int before = buf.readShort();
                int after = buf.readShort();

                Map wMap = new HashMap();
                wMap.put("weight", weight);
                wMap.put("before", before);
                wMap.put("after", after);
                list.add(wMap);
            }

            param.put("authType", authType);
            param.put("authContent", authContent);
            param.put("binsWeight", list);
            hwHeader.setParamMap(param);

            Object[] args = new Object[]{terminalId, authType, authContent, n, JacksonUtil.toJson(list), gwTime};
            String sql = "INSERT INTO veh_card_pick_log (ter_no, auth_type, auth_content, channel_count, pick_content, gw_time) VALUES (?, ?, ?, ?, ?, ?)";
            jdbcTemplate.update(sql, args);

            return;
        }

        if (0x06 == cmd) {
            int n = buf.readByte();
            if (buf.readableBytes() < n) {

                log.error("数据长度不足: [{}]", CommonUtil.bytesToStr(content));
            }

            int[] array = new int[n];
            for (int i = 0; i < n; i++) {
                array[i] = buf.readByte();
            }

            param.put("binsFault", array);
            hwHeader.setParamMap(param);

            Object[] args = new Object[]{terminalId, n, JacksonUtil.toJson(array), gwTime};
            String sql = "INSERT INTO veh_fault_log (ter_no, channel_count, fault_content, gw_time) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sql, args);

            return;
        }
    }

    @Override
    public byte[] pack(Header header, Object... argus) {
        HwHeader hwHeader = (HwHeader) header;
        int cmd = hwHeader.getCmd();

        byte[] startBytes = hwHeader.getStartBytes();

        if (0x03 == cmd || 0x06 == cmd || 0x07 == cmd) {
            ByteBuf buf = Unpooled.copiedBuffer(startBytes, new byte[]{(byte) cmd, 0x01, 0x01});
            return combine(buf.array());
        }

        if (0x04 == cmd) {
            int status = (int) argus[0];
            long account = (long) argus[1];
            String user = (String) argus[2];
            int money = (int) argus[3];

            byte[] userBytes = user.getBytes(Charset.forName("UTF-8"));
            int userLen = userBytes.length;
            byte[] userArray = new byte[20];
            System.arraycopy(userBytes, 0, userArray, 0, userLen);

            ByteBuf buf = Unpooled.buffer(40);
            buf.writeBytes(startBytes);
            buf.writeByte(cmd);
            buf.writeByte(35);
            buf.writeByte(status);
            buf.writeLong(account);
            buf.writeByte(userLen);
            buf.writeBytes(userArray);
            buf.writeInt(money);
            buf.writeByte(1);

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
