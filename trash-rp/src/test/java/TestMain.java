import cn.com.tiza.earth4j.GisData;
import cn.com.tiza.earth4j.LocationParser;
import cn.com.tiza.earth4j.entry.Location;
import com.alibaba.fastjson.JSON;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.JacksonUtil;
import org.junit.Test;

/**
 * Description: TestMain
 * Author: DIYILIU
 * Update: 2018-12-13 11:14
 */
public class TestMain {


    @Test
    public void test() throws Exception {

//        GisData gisData = new GisData();
//        gisData.load();
//
//        Location location =  gisData.getLocation(lng, lat);
//        System.out.println(location.getCity());

        LocationParser locationParser = LocationParser.getInstance();
        locationParser.init();

        double lat = 34.288785;
        double lng = 117.258730;

        Location location = locationParser.parse(lng, lat);
        System.out.println(JSON.toJSONString(location));
    }


    @Test
    public void test1(){

        String str = "00000000000031323334353637383930";

        byte[] content = CommonUtil.hexStringToBytes(str);

        int length = content.length;
        int index = 0;
        for (int  i = 0; i < length; i ++){
            if (content[i] > 0){
                index = i;
                break;
            }
        }
        String userId = CommonUtil.parseBytes(content, index, length - index);

        System.out.println(userId);

    }
}
