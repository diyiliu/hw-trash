package com.tiza.trash.rp.support.model;

import cn.com.tiza.earth4j.LocationParser;
import cn.com.tiza.tstar.common.process.BaseHandle;
import cn.com.tiza.tstar.common.process.RPTuple;
import com.tiza.plugin.cache.ICache;
import com.tiza.plugin.model.Header;
import com.tiza.plugin.model.facade.IDataProcess;
import com.tiza.plugin.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Description: BaseParseHandle
 * Author: DIYILIU
 * Update: 2019-06-25 19:15
 */

@Slf4j
public class BaseParseHandle extends BaseHandle {

    public boolean parse(String terminal, byte[] bytes, Header header, IDataProcess process){
        ICache vehicleInfoProvider = SpringUtil.getBean("vehicleInfoProvider");
        //log.info("设备缓存: {}", JacksonUtil.toJson(vehicleInfoProvider.getKeys()));
        // 验证设备是否绑定车辆
        if (!vehicleInfoProvider.containsKey(terminal)) {
            log.warn("设备[{}]未绑定车辆信息!", terminal);

            return false;
        }

        process.parse(bytes, header);
        return true;
    }


    @Override
    public RPTuple handle(RPTuple rpTuple) throws Exception {

        return null;
    }


    @Override
    public void init() throws Exception {
        // 加载地图数据，解析省市区
        LocationParser.getInstance().init();

        // 装载 Spring 容器
        SpringUtil.init();
    }
}
