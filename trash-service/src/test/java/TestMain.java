import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import com.tiza.service.support.model.CallInfo;
import com.tiza.service.support.model.CardInfo;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description: TestMain
 * Author: DIYILIU
 * Update: 2018-12-11 15:50
 */
public class TestMain {


    @Test
    public void test() {
        String str = "027029947553";

        System.out.println(str.length());

        CommonUtil.packBCD(str, 12);
    }

    @Test
    public void test1() throws Exception {
        String str = "{\"errcode\":0,\"name\":\"郭敬茜\",\"balance\":\"8442.00000\",\"uid\":\"15198\",\"phone\":\"13775991789\"}";

        CardInfo cardInfo = JacksonUtil.toObject(str, CardInfo.class);

        System.out.println(cardInfo.getName());
    }

    @Test
    public void test2() {

        String phone = "1368513";
        int length = phone.length();
        String str = phone.substring(0, 3) + phone.substring(length - 4, length);

        System.out.println(str);
    }


    @Test
    public void test3() {
        Double d = 100d;

        byte[] bytes = CommonUtil.longToBytes(d.intValue(), 3);
        System.out.println(CommonUtil.bytesToStr(bytes));
    }


    @Test
    public void test4() {
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


    @Test
    public void testAddZero() {
        String str = "abc";

        System.out.println(CommonUtil.addPrefixZero(str, 11));
    }


    @Test
    public void buildJson(){
        CallInfo callInfo = new CallInfo();
        callInfo.setUrl("http://47.107.166.242:8083");
        callInfo.setAppId("sdkwijw");
        callInfo.setAppSecret("4b93e958a900aea565a6a1582ce6ad81");
        callInfo.setKey("");
        callInfo.setName("中航环卫");

        List l1 = new ArrayList();
        l1.add(callInfo);

        CallInfo callInfo2 = new CallInfo();
        callInfo2.setUrl("http://zfsh.gltin.com/services/index.php");
        callInfo2.setAppId("45293c0b2356873b");
        callInfo2.setAppSecret("ee5e2e7d30f37a832a3c5e7806365eee");
        callInfo2.setKey("");
        callInfo2.setName("");

        List l2 = new ArrayList();
        l2.add(callInfo2);



        Map m = new HashMap();
        m.put("bin", l1);
        m.put("bag", l2);

        System.out.println(JacksonUtil.toJson(m));
    }

    @Test
    public void readJson() throws Exception{
        ResourceLoader loader = new DefaultResourceLoader();
        Resource resource = loader.getResource("classpath:config/data.json");

        String json = FileUtils.readFileToString(resource.getFile());
//        System.out.println(json);

        Map map = JacksonUtil.toObject(json, HashMap.class);

        List list = (List) map.get("bin");
        System.out.println(list.size());
        Map m = (Map) list.get(0);

        CallInfo callInfo = new CallInfo();
        BeanUtils.copyProperties(callInfo, m);

        System.out.println(callInfo.getName());



    }


    @Test
    public void testUser(){
        String str = "51464C4A3030303030303030303030303031303135303031";

        str = new String(CommonUtil.hexStringToBytes(str));
        System.out.println(str);
    }
}
