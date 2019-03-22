import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.service.support.model.CardInfo;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: TestMain
 * Author: DIYILIU
 * Update: 2018-12-11 15:50
 */
public class TestMain {


    @Test
    public void test(){
        String str = "027029947553";

        System.out.println(str.length());

        CommonUtil.packBCD(str, 12);
    }

    @Test
    public void test1() throws Exception{
        String str = "{\"errcode\":0,\"name\":\"郭敬茜\",\"balance\":\"8442.00000\",\"uid\":\"15198\",\"phone\":\"13775991789\"}";

        CardInfo cardInfo = JacksonUtil.toObject(str, CardInfo.class);

        System.out.println(cardInfo.getName());
    }

    @Test
    public void test2(){

        String phone = "1368513";
        int length = phone.length();
        String str = phone.substring(0, 3) + phone.substring(length - 4, length);

        System.out.println(str);
    }


    @Test
    public void test3(){
        Double d = 100d;

        byte[] bytes = CommonUtil.longToBytes(d.intValue(), 3);
        System.out.println(CommonUtil.bytesToStr(bytes));
    }


    @Test
    public void test4(){
        String str = "27029947554";

        System.out.println(CommonUtil.packBCD(str, 11));
    }


    @Test
    public void test5() {
        Map map = new HashMap() {
            {
                this.put("a", 123);
            }
        };

        String str = JacksonUtil.toJson(map);
        System.out.println(str);
    }
}
