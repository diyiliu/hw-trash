package com.tiza.rp.support.parse.process;

import com.tiza.plugin.model.Header;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.rp.support.model.HwHeader;
import com.tiza.rp.support.parse.HwDataProcess;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.*;

/**
 * Description: BagProcess
 * Author: DIYILIU
 * Update: 2018-12-10 14:24
 */

@Slf4j
@Service
public class BagProcess extends HwDataProcess {
    private final static String[] pidArray = {"1", "2", "3", "4", "5"};

    @Override
    public Header parseHeader(byte[] bytes) {
        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        // 0xF1
        buf.readByte();
        // 0x5B,0xB5
        buf.readBytes(new byte[2]);

        // 消息ID
        byte[] startBytes = new byte[2];
        buf.readBytes(startBytes);

        int length = buf.readUnsignedByte();
        if (buf.readableBytes() < length) {

            log.error("数据长度不足: [{}]", CommonUtil.bytesToStr(bytes));
            return null;
        }
        // 读写指令标识
        int readWrite = buf.readUnsignedByte();
        // 功能编号
        int funId = buf.readByte();
        // 数据
        byte[] content = new byte[length - 2];
        buf.readBytes(content);

        HwHeader header = new HwHeader();
        header.setStartBytes(startBytes);
        header.setReadWrite(readWrite);
        header.setCmd(funId);
        header.setContent(content);

        return header;
    }

    @Override
    public void parse(byte[] content, Header header) {
        HwHeader hwHeader = (HwHeader) header;

        int readWrite = hwHeader.getReadWrite();
        int cmd = hwHeader.getCmd();
        ByteBuf buf = Unpooled.copiedBuffer(content);

        Map param = new HashMap();
        if (0x20 == readWrite) {
            switch (cmd) {
                case 0x01:
                    int length = content.length;
                    int index = 0;
                    for (int i = 0; i < length; i++) {
                        if (content[i] > 0) {
                            index = i;
                            break;
                        }
                    }
                    String userId = CommonUtil.parseBytes(content, index, length - index);
                    param.put("userId", userId);

                    break;
                case 0x02:
                    param.put("password", new String(content));

                    break;
                case 0x03:
                    byte[] worker = new byte[6];
                    buf.readBytes(worker);

                    byte[] pwd = new byte[6];
                    buf.readBytes(pwd);
                    param.put("workerId", new String(worker));
                    param.put("workerPw", new String(pwd));

                    break;
                case 0x04:

                    List itemList = new ArrayList();
                    for (int i = 0; i < 5; i++) {
                        String pid = pidArray[i];
                        int code = buf.getByte(i);
                        int x = buf.getByte(5 + i);
                        int y = buf.getByte(6 + i);

                        Map item = new HashMap();
                        item.put("pid", pid);
                        item.put("code", code);
                        item.put("x", x);
                        item.put("y", y);
                        itemList.add(item);
                    }
                    param.put("bagPush", JacksonUtil.toJson(itemList));

                    break;
                case 0x05:
                    List putList = new ArrayList();
                    for (int i = 0; i < content.length; i++) {
                        if (content[i] > 0) {
                            putList.add(i + 1);
                        }
                    }
                    param.put("bagPut", JacksonUtil.toJson(putList));

                    break;
                default:
                    log.warn("功能编号[{}]异常!", cmd);
            }

            hwHeader.setParamMap(param);
            return;
        }

        if (0x40 == readWrite) {
            if (cmd > 3) {
                log.warn("功能编号[{}]异常!", cmd);
            }

            hwHeader.setParamMap(param);
            return;
        }
    }


    @Override
    public byte[] pack(Header header, Object... argus) {
        HwHeader hwHeader = (HwHeader) header;

        int readWrite = hwHeader.getReadWrite();
        int cmd = hwHeader.getCmd();

        if (0x20 == readWrite) {
            int result = (int) argus[0];

            ByteBuf buf = Unpooled.buffer(6);
            buf.writeBytes(hwHeader.getStartBytes());
            buf.writeByte(3);
            buf.writeByte(0x2B);
            buf.writeByte(hwHeader.getCmd());
            buf.writeByte(result);

            return combine(buf.array());
        }

        if (0x40 == readWrite) {
            byte[] bytes = new byte[0];
            switch (cmd) {
                case 0x01:
                    String terminal = hwHeader.getTerminalId();
                    int strLen = terminal.length();
                    // 截取
                    if (strLen > 11){
                        terminal = terminal.substring(0, 11);
                    }
                    // 补足
                    if (strLen < 11){
                        terminal = String.format("%0" + (11 - strLen) + "d", 0) + terminal;
                    }
                    bytes = terminal.getBytes();

                    break;
                case 0x02:
                    String phone = (String) argus[0];
                    Double balance = (double) argus[1];
                    String name = (String) argus[2];

                    int length = phone.length();
                    if (length > 7) {
                        phone = phone.substring(0, 3) + phone.substring(length - 4, length);
                    }
                    // 补足
                    if (length < 7){
                        phone = String.format("%0" + (7 - length) + "d", 0) + phone;
                    }

                    byte[] phoneBytes = phone.getBytes();
                    byte[] balanceBytes = CommonUtil.longToBytes(balance.intValue(), 3);

                    byte[] nameBytes = new byte[6];
                    name = name.substring(1);
                    byte[] nameArray = name.getBytes(Charset.forName("GB2312"));
                    int len = nameArray.length > 6 ? 6 : nameArray.length;
                    System.arraycopy(nameArray, 0, nameBytes, 0, len);
                    // 组装数据
                    bytes = Unpooled.copiedBuffer(phoneBytes, balanceBytes, nameBytes).array();

                    break;
                case 0x03:
                    List<Map> dataList = (List<Map>) argus[0];
                    Map dataMap = new HashMap();
                    for (Map map : dataList) {
                        String pid = (String) map.get("pid");
                        dataMap.put(pid, map);
                    }

                    int count = pidArray.length;
                    // 积分 + 是否可用
                    ByteBuf priceBuf = Unpooled.buffer(count * 2 + 1);
                    ByteBuf xyBuf = Unpooled.buffer(count * 2);
                    int enable = 0;
                    for (int i = 0; i < count; i++) {
                        String pid = pidArray[i];
                        Map item = (Map) dataMap.get(pid);

                        Double price = Double.valueOf(String.valueOf(item.get("price")));
                        priceBuf.writeShort(price.intValue());

                        int status = Integer.valueOf(String.valueOf(item.get("enable")));
                        if (status == 1) {
                            enable |= 1 << i;
                        }

                        int x = Integer.valueOf(String.valueOf(item.get("x")));
                        int y = Integer.valueOf(String.valueOf(item.get("y")));
                        xyBuf.writeByte(x);
                        xyBuf.writeByte(y);
                    }
                    // 加入一个字节的 可用状态
                    priceBuf.writeByte(enable);
                    // 组装数据
                    bytes = Unpooled.copiedBuffer(priceBuf, xyBuf).array();

                    break;
                default:
                    break;
            }

            if (bytes == null || bytes.length < 1) {
                return null;
            }

            int length = bytes.length;
            ByteBuf buf = Unpooled.buffer(5 + length);
            buf.writeBytes(hwHeader.getStartBytes());
            buf.writeByte(length + 2);
            buf.writeByte(0x4B);
            buf.writeByte(hwHeader.getCmd());
            buf.writeBytes(bytes);

            return combine(buf.array());
        }

        return null;
    }

    public byte[] combine(byte[] content) {
        ByteBuf buf = Unpooled.buffer(3 + content.length);
        buf.writeByte(0xF1);
        buf.writeByte(0x5B);
        buf.writeByte(0xB5);
        buf.writeBytes(content);

        return buf.array();
    }
}
