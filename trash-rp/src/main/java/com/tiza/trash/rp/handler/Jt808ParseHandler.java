package com.tiza.trash.rp.handler;

import cn.com.tiza.tstar.common.process.RPTuple;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.model.Jt808Header;
import com.tiza.plugin.protocol.jt808.Jt808DataProcess;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.plugin.util.SpringUtil;
import com.tiza.trash.rp.support.model.BaseParseHandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;

/**
 * Description: Jt808ParseHandler
 * Author: DIYILIU
 * Update: 2018-12-06 10:18
 */

@Slf4j
public class Jt808ParseHandler extends BaseParseHandle {

    @Override
    public RPTuple handle(RPTuple rpTuple) {
        log.info("终端[{}], 指令[{}]...", rpTuple.getTerminalID(), CommonUtil.toHex(rpTuple.getCmdID(), 4));

        ICache cmdCacheProvider = SpringUtil.getBean("cmdCacheProvider");
        Jt808DataProcess process = (Jt808DataProcess) cmdCacheProvider.get(rpTuple.getCmdID());
        if (process == null) {
            log.warn("CMD {}, 找不到指令[{}]解析器!", JacksonUtil.toJson(cmdCacheProvider.getKeys()), CommonUtil.toHex(rpTuple.getCmdID(), 4));
            return null;
        }

        try {
            // 解析消息头
            Jt808Header header = (Jt808Header) process.parseHeader(rpTuple.getMsgBody());
            if (header != null) {
                String terminal = header.getTerminalId();

                header.setGwTime(rpTuple.getTime());
                if (parse(terminal, header.getContent(), header, process)) {
                    return rpTuple;
                }
            }
        } catch (BeansException e) {
            e.printStackTrace();
        }

        return rpTuple;
    }

}
