import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

/**
 * Description: TestHttp
 * Author: DIYILIU
 * Update: 2018-12-20 17:17
 */
public class TestHttp {

    public static String url = "http://zfsh.gltin.com/services/index.php";

    @Test
    public void test1() throws Exception{
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
}
