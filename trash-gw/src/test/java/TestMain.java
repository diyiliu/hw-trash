import com.tiza.gw.jt808.Jt808Decoder;
import com.tiza.plugin.util.CommonUtil;
import handler.JT808TestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * Description: TestMain
 * Author: DIYILIU
 * Update: 2018-11-28 16:42
 */

@Slf4j
public class TestMain {

    public static void main(String[] args) {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();

            b.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new Jt808Decoder())
                                    .addLast(new JT808TestHandler());
                        }
                    });
            ChannelFuture f = b.bind(5500).sync();
            System.out.println("JT808服务器启动...");
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }


    @Test
    public void test(){
        String str = "7E00010005027029947553016D0000810000007E";

        System.out.println(str.replaceAll(" ", ""));

        str = "027029947553";

        String terminalId = CommonUtil.bytesToStr(CommonUtil.hexStringToBytes(str));
        System.out.println(terminalId);
    }


    @Test
    public void test1(){
        byte[] bytes = CommonUtil.hexStringToBytes("31 32 33 34 35 36 ");

        System.out.println(new String(bytes));
    }


    @Test
    public void test2(){
        String str = "D0D0";

        byte[] bytes = CommonUtil.packBCD(str, 12);
        System.out.println(CommonUtil.bytesToStr(bytes));
    }


    @Test
    public void test3(){

        String str = "027029947553";

        System.out.println(str.replaceFirst("^0*", ""));
    }
}
