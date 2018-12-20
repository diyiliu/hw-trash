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

import java.util.*;

/**
 * Description: BagProcess
 * Author: DIYILIU
 * Update: 2018-12-10 14:24
 */

@Slf4j
@Service
public class BagProcess extends HwDataProcess {

    @Override
    public Header parseHeader(byte[] bytes) {
        ByteBuf buf = Unpooled.copiedBuffer(bytes);
        // 0xF1
        buf.readByte();
        // 0x5B,0xB5
        buf.readBytes(new byte[2]);

        // 消息ID
        int msgId = buf.readUnsignedShort();
        int length = buf.readUnsignedByte();
        if (buf.readableBytes() < length){

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
        header.setMsgId(msgId);
        header.setReadWrite(readWrite);
        header.setCmd(funId);
        header.setContent(content);

        return header;
    }

    @Override
    public void parse(byte[] content, Header header) {
        HwHeader hwHeader = (HwHeader) header;
        String terminalId = hwHeader.getTerminalId();
        Date gwTime = new Date(hwHeader.getTime());

        int readWrite = hwHeader.getReadWrite();
        int cmd = hwHeader.getCmd();
        ByteBuf buf = Unpooled.copiedBuffer(content);

        Map param = new HashMap();
        param.put("msgId", hwHeader.getMsgId());
        param.put("readWrite", hwHeader.getReadWrite());
        if (readWrite == 0x20){
            switch (cmd){
                case 0x01:
                    int length = content.length;
                    int index = 0;
                    for (int  i = 0; i < length; i ++){
                        if (content[i] > 0){
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
                    String[] pidArray = {"1", "2", "3", "4", "5"};

                    List itemList = new ArrayList();
                    for (int i = 0; i < 5; i++){
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
                    for (int i = 0; i < content.length; i++){
                        if (content[i] > 0){
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

        if (readWrite == 0x40){
            if (cmd > 3){
                log.warn("功能编号[{}]异常!", cmd);
            }

            return;
        }
    }
}
