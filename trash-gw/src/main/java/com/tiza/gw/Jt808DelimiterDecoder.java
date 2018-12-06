package com.tiza.gw;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import java.util.List;

/**
 * Description: Jt808DelimiterDecoder
 * Author: DIYILIU
 * Update: 2018-11-29 09:59
 */
public class Jt808DelimiterDecoder extends DelimiterBasedFrameDecoder {

    public Jt808DelimiterDecoder() {
        super(1024, Unpooled.copiedBuffer(new byte[]{0x7E}), Unpooled.copiedBuffer(new byte[]{0x7E, 0x7E}));
    }

    /**
     * 复用DelimiterBasedFrameDecoder decode方法
     */
    public void tstarDecode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {

        decode(ctx, buf, out);
    }
}
