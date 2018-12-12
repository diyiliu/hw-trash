import com.tiza.plugin.util.CommonUtil;
import org.junit.Test;

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
}
