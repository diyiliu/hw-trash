package com.tiza.rp.support.model;

import com.tiza.plugin.model.Header;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * Description: HwHeader
 * Author: DIYILIU
 * Update: 2018-12-10 14:53
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class HwHeader extends Header {

    private String terminalId;

    private byte[] startBytes;

    private int cmd;

    private byte[] content;

    private byte check;

    private byte endByte;

    private Map paramMap;

    private long time;
}
