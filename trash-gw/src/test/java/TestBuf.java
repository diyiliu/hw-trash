import com.tiza.plugin.util.CommonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

/**
 * Description: TestBuf
 * Author: DIYILIU
 * Update: 2018-11-28 16:07
 */
public class TestBuf {


    @Test
    public void test() {

        String str = "002c0133373039363054";

        byte[] bytes = CommonUtil.hexStringToBytes(str);

        ByteBuf buf = Unpooled.copiedBuffer(bytes);

        ByteBuf buf1 = buf.slice();
        buf1.readInt();

        ByteBuf buf2 = buf.duplicate();
        buf2.readInt();


//        ByteBuf duplicate = buf.duplicate();
//
//        ByteBuf sliced = buf.slice(0, 5);
//
//        ByteBuf readSlice = buf.readSlice(5);

        buf = Unpooled.buffer(0);
        System.out.println(buf);
        System.out.println(buf1);
    }
}
