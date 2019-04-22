package com.tiza.service.support.model;

import com.tiza.plugin.util.HttpUtil;
import com.tiza.plugin.util.JacksonUtil;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: CallInfo
 * Author: DIYILIU
 * Update: 2019-04-22 11:10
 */

@Data
public class CallInfo {
    private long expire;

    private String name;

    private String key;

    private String url;

    private String appId;

    private String appSecret;

    private String token;

    /** 请求 token 的路径 **/
    private String tokenPath;

    /** 获取 token Json 中的 key **/
    private String tokenKey;

    public String getToken(){
        if (System.currentTimeMillis() > expire){
            Map param = new HashMap();
            param.put("appid", appId);
            param.put("secret", appSecret);
            try {
                String json = HttpUtil.getForString(url + tokenPath, param);

                Map map = JacksonUtil.toObject(json, HashMap.class);
                int errcode = (int) map.get("errcode");
                if (0 == errcode) {
                    expire = System.currentTimeMillis() + (int) map.get("expires") * 1000;

                    return  (String) map.get(tokenKey);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return token;
    }
}
