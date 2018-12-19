package com.tiza.rp.support.parse;

import com.tiza.plugin.model.Header;
import com.tiza.plugin.model.IDataProcess;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Description: HwDataProcess
 * Author: DIYILIU
 * Update: 2018-12-10 14:21
 */

@Service
public class HwDataProcess implements IDataProcess {

    @Resource
    protected JdbcTemplate jdbcTemplate;

    protected String hwKey;

    protected Integer vehType;

    private Map<Integer, HwDataProcess> hwProcessMap = new HashMap();

    @Override
    public Header parseHeader(byte[] bytes) {
        return null;
    }

    @Override
    public void parse(byte[] content, Header header) {

    }

    @Override
    public byte[] pack(Header header, Object... argus) {
        return new byte[0];
    }

    @Override
    public void init() {
        hwProcessMap.put(vehType, this);
    }

    public HwDataProcess getDataProcess(int vehType){

        return hwProcessMap.get(vehType);
    }

    public String getHwKey() {
        return hwKey;
    }
}
