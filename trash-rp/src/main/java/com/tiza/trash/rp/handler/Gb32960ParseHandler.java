package com.tiza.trash.rp.handler;

import cn.com.tiza.tstar.common.process.RPTuple;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.model.Gb32960Header;
import com.tiza.plugin.protocol.gb32960.Gb32960DataProcess;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.plugin.util.SpringUtil;
import com.tiza.trash.rp.support.model.BaseParseHandle;
import lombok.extern.slf4j.Slf4j;

/**
 * Description: Gb32960ParseHandler
 * Author: DIYILIU
 * Update: 2018-12-06 10:18
 */

@Slf4j
public class Gb32960ParseHandler extends BaseParseHandle {

    @Override
    public RPTuple handle(RPTuple rpTuple) {
        log.info("终端[{}], 指令[{}]...", rpTuple.getTerminalID(), CommonUtil.toHex(rpTuple.getCmdID(), 2));
        ICache cmdCacheProvider = SpringUtil.getBean("cmdCacheProvider");
        Gb32960DataProcess process = (Gb32960DataProcess) cmdCacheProvider.get(rpTuple.getCmdID());
        if (process == null) {
            log.warn("CMD {}, 找不到指令[{}]解析器!", JacksonUtil.toJson(cmdCacheProvider.getKeys()), CommonUtil.toHex(rpTuple.getCmdID(), 2));
            return null;
        }

        // 解析消息头
        Gb32960Header header = (Gb32960Header) process.parseHeader(rpTuple.getMsgBody());
        if (header != null) {
            String terminal = header.getVin();

            header.setGwTime(rpTuple.getTime());
            if (parse(terminal, header.getContent(), header, process)) {
                return rpTuple;
            }
        }

        return null;
    }
}
