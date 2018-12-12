import cn.com.tiza.tstar.datainterface.client.TStarStandardClient;
import cn.com.tiza.tstar.datainterface.client.entity.ClientCmdSendResult;
import com.tiza.plugin.util.CommonUtil;
import com.tiza.plugin.util.DateUtil;
import org.junit.Test;

import java.util.Date;

/**
 * Description: TestClient
 * Author: DIYILIU
 * Update: 2018-12-07 11:01
 */
public class TestClient {


    @Test
    public void test() throws Exception{
        String str = "7E8001000502702994755308DE02D9000200627E";
        byte[] content = CommonUtil.hexStringToBytes(str);

        System.out.println("执行：" + DateUtil.dateToString(new Date()));
        long t = System.currentTimeMillis();
        TStarStandardClient tStarClient = new TStarStandardClient("trash", "123456");
        long second = (System.currentTimeMillis() - t) / 1000;
        System.out.println("初始化用时(秒)：" + second);

        t = System.currentTimeMillis();
        // TStar 指令下发
        ClientCmdSendResult sendResult = tStarClient.cmdSend("trash_jt808", "027029947553",
                32769, 123456, content, 1);
        second = (System.currentTimeMillis() - t) / 1000;
        System.out.println("结果：" + sendResult.getIsSuccess() + "，用时(秒)：" + second);
    }
}
