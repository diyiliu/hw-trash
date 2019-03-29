package com.tiza.rp.support.model;

import lombok.Data;

import java.util.Map;

/**
 * Description: SendData
 * Author: DIYILIU
 * Update: 2018-12-21 13:16
 */

@Data
public class SendData {

    /** 终端ID **/
    private String terminal;

    /** 透传指令ID **/
    private Integer cmd;

    /** 透传指令应答ID **/
    private Integer respCmd;

    /** 透传内容 **/
    private String content;

    /** 透传解析参数 **/
    private Map data;

    /** 解析时间 **/
    private Long time;
}
