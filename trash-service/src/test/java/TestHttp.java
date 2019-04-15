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


    public static String url2 = "http://47.107.166.242:8083/";

    @Test
    public void test2() throws Exception {
        Map map = new HashMap() {
            {
                this.put("appid", "sdkwijw");
                this.put("secret", "4b93e958a900aea565a6a1582ce6ad81");
            }
        };

        String result = HttpUtil.getForString(url2 + "token", map);

        System.out.println(result);
    }

    @Test
    public void test3() throws Exception {
        Map map = new HashMap() {
            {
                this.put("token", "e7365f1a-ee2e-409a-8a4a-df9ed468e9d7");
                this.put("type", "1");
                this.put("auth", "2000000000014421");
            }
        };

        String result = HttpUtil.getForString("http://47.107.166.242:8083/userinfo", map);

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
                this.put("type", "4");
                this.put("auth", "");
                this.put("data", list);
            }
        };

        Map param = new HashMap() {
            {
                this.put("token", "c9ca4da2-9668-4038-8e66-6c9a25808813");
                this.put("device", "27029947555" + "7511837323734");
            }
        };

        String result = HttpUtil.postWithJsonAndParameter(url2 +"throwin", JacksonUtil.toJson(map), param);
        System.out.println(result);
    }

    @Test
    public void test5() throws Exception {

        Map map = new HashMap() {
            {
                this.put("token", "9e19fabe-d9bc-471e-b59a-7c5940a09b16");
                this.put("device", "002B001A3236510C38373939");
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
                this.put("token", "2c44ab47-ac29-4069-84cb-539d38a4af3b");
                this.put("device", "002B001A3236510C38373939");
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

}
