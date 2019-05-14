import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.HttpUtil;
import com.tiza.plugin.util.JacksonUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: TestBag
 * Author: DIYILIU
 * Update: 2019-05-10 09:51
 */
public class TestBag {

    public static String url = "http://192.168.1.180:9580/trashApi/receive/bag";


    public static String getTicket() throws Exception {
        Map map = new HashMap() {
            {
                this.put("action", "ticket");
                this.put("appid", "TZ0872");
                this.put("secret", "TIZA123456");
            }
        };

        String result = HttpUtil.getForString(url, map);
        Map resMap = JacksonUtil.toObject(result, HashMap.class);
        if (0 == (int) resMap.get("errcode")) {

            String ticket = (String) resMap.get("ticket");
            System.out.println("票据:" + ticket);
            return ticket;
        }

        System.out.println("获取票据异常!");

        return null;
    }

    @Test
    public void testTicket() throws Exception {
        System.out.println(getTicket());
    }

    @Test
    public void testCardInfo() throws Exception {

        Map map = new HashMap() {
            {
                this.put("action", "cardinfo");
                this.put("ticket", getTicket());
                this.put("idcard", "1025");
                this.put("devno", "27029947555");
            }
        };

        String result = HttpUtil.getForString(url, map);
        System.out.println(result);
    }

    public static String authCard(final String ticket) throws Exception {
        Map map = new HashMap() {
            {
                this.put("action", "authcard");
                this.put("ticket", ticket);
                this.put("idcard", "1025");
                this.put("pwd", "qwer1234");
                this.put("devno", "27029947555");
            }
        };

        String result = HttpUtil.getForString(url, map);
        Map resMap = JacksonUtil.toObject(result, HashMap.class);
        if (0 == CommonUtil.convertInt(resMap.get("errcode"))) {

            String token = (String) resMap.get("token");
            System.out.println("令牌:" + token);
            return token;
        }

        System.out.println("鉴权积分卡异常!");
        return null;
    }

    @Test
    public void testAuthcard() throws Exception {

        System.out.println(authCard(getTicket()));
    }

    @Test
    public void testProduct() throws Exception {

        final String ticket = getTicket();
        Map map = new HashMap() {
            {
                this.put("action", "product");
                this.put("ticket", ticket);
                this.put("token", authCard(ticket));
                this.put("idcard", "1025");
                this.put("devno", "27029947555");
            }
        };

        String result = HttpUtil.getForString(url, map);
        System.out.println(result);
    }
}
