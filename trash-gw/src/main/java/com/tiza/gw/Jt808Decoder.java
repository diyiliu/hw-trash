package com.tiza.gw;

import cn.com.tiza.tstar.gateway.codec.CustomDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

/**
 * Description: Jt808Decoder
 * Author: DIYILIU
 * Update: 2018-11-28 14:06
 */

public class Jt808Decoder extends CustomDecoder {

    private Jt808DelimiterDecoder delimiterDecoder;

    public Jt808Decoder() {
        delimiterDecoder = new Jt808DelimiterDecoder();
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {

        delimiterDecoder.tstarDecode(ctx, buf, out);
    }
}
