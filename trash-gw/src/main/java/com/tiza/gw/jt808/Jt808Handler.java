package com.tiza.gw.jt808;

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
        // 过滤空(垃圾)数据
        if (bytes.length < 12){

            return null;
        }

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
        // 去除前导 0
        String terminalId = CommonUtil.bytesToStr(array).replaceFirst("^0*", "");
        int serial = buf.readUnsignedShort();

        buf.readBytes(new byte[length]);
        byte check = buf.readByte();
        if (check != realCheck){
            log.error("校验位验证失败[{}, {}]！", CommonUtil.toHex(check), CommonUtil.toHex(realCheck));
            return null;
        }

        TStarData tStarData = new TStarData();
        tStarData.setMsgBody(Unpooled.copiedBuffer(new byte[]{0x7E}, content, new byte[]{check, 0x7E}).array());
        tStarData.setCmdID(cmd);
        tStarData.setTerminalID(terminalId);
        tStarData.setCmdSerialNo(serial);
        tStarData.setTime(System.currentTimeMillis());


        // 下行自动生成得 序列号
        int msgSerial = CommonUtil.getMsgSerial();
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

            byte[] respMsg = CommonUtil.jt808Response(terminalId, bf.array(), 0x8100, msgSerial);
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

            byte[] respMsg = CommonUtil.jt808Response(terminalId, bf.array(), 0x8001, msgSerial);
            respData.setCmdID(0x8001);
            respData.setMsgBody(respMsg);
            context.channel().writeAndFlush(respData);
        }

        return tStarData;
    }

    @Override
    public void commandReceived(ChannelHandlerContext ctx, CommandData cmd) {

        log.info("下行消息，终端[{}]指令[{}], 内容[{}]...", cmd.getTerminalID(), CommonUtil.toHex(cmd.getCmdID(), 4), CommonUtil.bytesToStr(cmd.getMsgBody()));
    }
}
