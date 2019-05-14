import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.HttpUtil;
import com.tiza.plugin.util.JacksonUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description: TestHttp
 * Author: DIYILIU
 * Update: 2018-12-20 17:17
 */
public class TestHttp {

    public static String url = "http://zfsh.gltin.com/services/index.php";

    @Test
    public void test1() throws Exception {
        URIBuilder builder = new URIBuilder(url);
        builder.addParameter("action", "ticket");
        builder.addParameter("appid", "45293c0b2356873b");
        builder.addParameter("secret", "ee5e2e7d30f37a832a3c5e7806365eee");

        HttpGet httpGet = new HttpGet(builder.build());
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String result = EntityUtils.toString(entity, "UTF-8");

        System.out.println(result);
    }


//    public static String url2 = "http://47.107.166.242:8083/";
//    public static String url2 = "http://58.213.118.25:8081/trashApi/";
    public static String url2 = "http://192.168.1.180:9580/trashApi/";


    @Test
    public void test2() throws Exception {
        Map map = new HashMap() {
            {
                this.put("appid", "TZ0872");
                this.put("secret", "TIZA123456");
//                this.put("appid", "sdkwijw");
//                this.put("secret", "4b93e958a900aea565a6a1582ce6ad81");
            }
        };

        String result = HttpUtil.getForString(url2 + "token", map);
        System.out.println(result);
    }

    @Test
    public void test3() throws Exception {
        Map map = new HashMap() {
            {
                this.put("token", "AE72BFC1DB974AB49879AC3E5FC3041E");
//                this.put("type", "1");
//                this.put("auth", "1016");
//                this.put("device", "1000094");
                this.put("type", "5");
                this.put("auth", "QFLJ20000000000001016216");
                this.put("device", "27029947555");
            }
        };

        String result = HttpUtil.getForString(url2 + "userinfo", map);

        System.out.println(result);
    }


    @Test
    public void test4() throws Exception {

        final List list = new ArrayList() {
            {
                this.add(new HashMap() {
                    {
                        this.put("category", 0);
                        this.put("weight", 0);
                        this.put("before", 4.91);
                        this.put("after", 4.91);
                    }
                });
                this.add(new HashMap() {
                    {
                        this.put("category", 1);
                        this.put("weight", 0);
                        this.put("before", 4.03);
                        this.put("after", 4.03);
                    }
                });
            }
        };

        Map map = new HashMap() {
            {
                this.put("type", "6");
                this.put("auth", "QFLJ10000000000001016538");
                this.put("data", list);
            }
        };

        Map param = new HashMap() {
            {
                this.put("token", "6DD2CFA926F3441FBB00BFB0D4258D6F");
                this.put("device", "27029947555");
            }
        };

        String result = HttpUtil.postWithJsonAndParameter(url2 +"throwin", JacksonUtil.toJson(map), param);
        System.out.println(result);
    }

    @Test
    public void test5() throws Exception {

        Map map = new HashMap() {
            {
                this.put("token", "2F129817F7FE40708B6E70FD15F13693");
                this.put("device", "1000094");
            }
        };

        List list = new ArrayList() {
            {
                this.add(0x14);
                this.add(0x14);
                this.add(0x14);
            }
        };

        String result = HttpUtil.postWithJsonAndParameter(url2 + "fault", JacksonUtil.toJson(list), map);
        System.out.println(result);
    }

    @Test
    public void test6() throws Exception {

        Map map = new HashMap() {
            {
                this.put("token", "EB085BE944DF44D9B65F71C9E1BF2B77");
                this.put("device", "27029947555");
                this.put("temperature", "10");
                this.put("capacities", "10,20,30");
            }
        };


        String result = HttpUtil.postForString(url2 + "/status", map);
        System.out.println(result);
    }

    @Test
    public void test7() throws Exception {

        Map map = new HashMap() {
            {
                this.put("token", "a88483d7-ef6a-4e7e-b2dc-93d9da09fe73");
                this.put("device", "002B001A3236510C38373939");
                this.put("type", "2");
                this.put("auth", "1000000000000229");
            }
        };


        String result = HttpUtil.getForString(url2 + "/clean", map);
        System.out.println(result);
    }

    @Test
    public void test8(){
        String str = "000e7581d39111e8afd500163e08fb5b";
        System.out.println(str.getBytes().length);

        System.out.println(JacksonUtil.toJson(str));
    }

    @Test
    public void test9(){

        String str = "51464C4A3132303030303030303030303134353434353531";

        str = new String(CommonUtil.hexStringToBytes(str));
        System.out.println(str);
    }

    @Test
    public void testGet() throws Exception{
        String url = "http://192.168.1.180:9580/trashApi?act=123";
        Map map = new HashMap() {
            {
                this.put("device", "002B001A3236510C38373939");
                this.put("type", "2");
                this.put("auth", "1000000000000229");
            }
        };

        System.out.println(HttpUtil.getForString(url, map));
    }
}
