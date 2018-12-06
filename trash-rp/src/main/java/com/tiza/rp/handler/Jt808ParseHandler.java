package com.tiza.rp.handler;

import cn.com.tiza.tstar.common.process.BaseHandle;
import cn.com.tiza.tstar.common.process.RPTuple;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.model.Jt808Header;
import com.tiza.plugin.protocol.jt808.Jt808DataProcess;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Description: Jt808ParseHandler
 * Author: DIYILIU
 * Update: 2018-12-06 10:18
 */

@Slf4j
public class Jt808ParseHandler extends BaseHandle {

    @Override
    public RPTuple handle(RPTuple rpTuple) throws Exception {
        log.info("终端[{}], 指令[{}]...", rpTuple.getTerminalID(), CommonUtil.toHex(rpTuple.getCmdID(), 4));

        ICache cmdCacheProvider = SpringUtil.getBean("cmdCacheProvider");
        Jt808DataProcess process = (Jt808DataProcess) cmdCacheProvider.get(rpTuple.getCmdID());
        if (process == null) {
            log.error("找不到指令[{}]解析器!", CommonUtil.toHex(rpTuple.getCmdID(), 4));
            return null;
        }

        Jt808Header header = (Jt808Header) process.parseHeader(rpTuple.getMsgBody());
        process.parse(header.getContent(), header);

        return rpTuple;
    }

    @Override
    public void init() throws Exception {
        // 装载Spring容器
        SpringUtil.init();
    }
}
