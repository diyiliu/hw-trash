package com.tiza.gw;

import cn.com.tiza.tstar.common.entity.TStarData;
import cn.com.tiza.tstar.gateway.entity.CommandData;
import cn.com.tiza.tstar.gateway.handler.BaseUserDefinedHandler;
import com.tiza.plugin.model.Jt808Header;
import com.tiza.plugin.util.CommonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description: Jt808Handler
 * Author: DIYILIU
 * Update: 2018-11-28 16:32
 */

@Slf4j
public class Jt808Handler extends BaseUserDefinedHandler {

    private List<Integer> respCmds;

    public Jt808Handler() {
        // 响应指令
        respCmds = new ArrayList() {
            {
                this.add(0x0002);
                this.add(0x0003);
                this.add(0x0102);
                this.add(0x0200);
                this.add(0x0704);
            }
        };
    }

    @Override
    public TStarData handleRecvMessage(ChannelHandlerContext context, ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        // 转义
        bytes = CommonUtil.decoderJt808Format(bytes);
        ByteBuf buf = Unpooled.copiedBuffer(bytes);

        byte[] content = new byte[bytes.length - 1];
        buf.getBytes(0, content);
        byte realCheck = CommonUtil.getCheck(content);

        int cmd = buf.readUnsignedShort();
        int bodyProperty = buf.readUnsignedShort();
        Jt808Header header = CommonUtil.parseJt808Body(bodyProperty);

        int length = header.getLength();

        byte[] array = new byte[6];
        buf.readBytes(array);
        String terminalId = CommonUtil.bytesToStr(array);
        int serial = buf.readUnsignedShort();

        buf.readBytes(new byte[length]);
        byte check = buf.readByte();
        if (check != realCheck){
            log.error("校验位验证失败[{}, {}]！", CommonUtil.toHex(check), CommonUtil.toHex(realCheck, 4));
            return null;
        }

        TStarData tStarData = new TStarData();
        tStarData.setMsgBody(Unpooled.copiedBuffer(new byte[]{0x7E}, content, new byte[]{check, 0x7E}).array());
        tStarData.setCmdID(cmd);
        tStarData.setTerminalID(terminalId);
        tStarData.setCmdSerialNo(serial);
        tStarData.setTime(System.currentTimeMillis());


        // 下行自动生成得 序列号
        int msgSerial = getMsgSerial();
        TStarData respData = new TStarData();
        respData.setTerminalID(terminalId);
        respData.setCmdSerialNo(msgSerial);
        respData.setTime(System.currentTimeMillis());

        // 注册
        if (0x0100 == cmd){
            byte[] auth = "123456".getBytes();
            ByteBuf bf = Unpooled.buffer(3 + auth.length);

            bf.writeShort(serial);
            bf.writeByte(0);
            bf.writeBytes(auth);

            byte[] respMsg = createResp(terminalId, bf.array(), 0x8100, msgSerial);
            respData.setCmdID(0x8100);
            respData.setMsgBody(respMsg);
            context.channel().writeAndFlush(respData);

            return tStarData;
        }

        // 通用应答
        if (respCmds.contains(cmd)){
            ByteBuf bf = Unpooled.buffer(5);
            bf.writeShort(serial);
            bf.writeShort(cmd);
            bf.writeByte(0);

            byte[] respMsg = createResp(terminalId, bf.array(), 0x8001, msgSerial);
            respData.setCmdID(0x8001);
            respData.setMsgBody(respMsg);
            context.channel().writeAndFlush(respData);
        }

        return tStarData;
    }

    /**
     * 命令序号
     **/
    private AtomicLong msgSerial = new AtomicLong(0);

    public int getMsgSerial() {
        Long serial = msgSerial.incrementAndGet();
        if (serial > 65535) {
            msgSerial.set(0);
            serial = msgSerial.incrementAndGet();
        }

        return serial.intValue();
    }

    @Override
    public void commandReceived(ChannelHandlerContext ctx, CommandData cmd) {

        log.info("下行消息，终端[{}]指令[{}], 内容[{}]...", cmd.getTerminalID(), CommonUtil.toHex(cmd.getCmdID(), 4), CommonUtil.bytesToStr(cmd.getMsgBody()));
    }

    /**
     * 生成回复指令内容
     *
     * @param terminal  设备ID
     * @param content   需要下行的指令内容
     * @param cmd       需要下行的命令ID
     * @param serial    下行的序列号
     * @return
     */
    private byte[] createResp(String terminal, byte[] content, int cmd, int serial) {
        // 消息体长度
        int length = content.length;

        Jt808Header header = new Jt808Header();
        // 不分包
        header.setSplit((byte) 0);
        // 不加密
        header.setEncrypt(0);
        header.setLength(length);
        header.setTerminalId(terminal);
        header.setContent(content);
        header.setCmd(cmd);
        header.setSerial(serial);

        byte[] bytes = CommonUtil.jt808HeaderToContent(header);
        byte check = CommonUtil.getCheck(bytes);
        // 添加校验位
        byte[] array = Unpooled.copiedBuffer(bytes, new byte[]{check}).array();
        // 转义
        array = CommonUtil.encoderJt808Format(array);

        // 添加标识位
        return Unpooled.copiedBuffer(new byte[]{0x7E}, array, new byte[]{0x7E}).array();
    }
}
