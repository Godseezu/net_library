package lc.com.net_library.tcpudp.util;

import java.nio.ByteBuffer;

/**
 * Created by Administrator on 2015/9/16.
 */
public class DataUtils {
    /**
     * int  转  byte[]
     * @param res
     * @return
     */
    public static byte[] int2byte(int res) {
        byte[] targets = new byte[4];
        targets[0] = (byte) (res & 0xff);// 最低位
        targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
        targets[2] = (byte) ((res >> 16) & 0xff);// 次高位
        targets[3] = (byte) (res >>> 24);// 最高位,无符号右移。
        return targets;
    }

    /**
     * byte [] 转 int
     * @param data
     * @return
     */
    public static int byteArray2int(byte [] data,int offset){
        ByteBuffer mbuffer=ByteBuffer.allocate(4);
        mbuffer.put(data,offset,4);
        mbuffer.flip();
        return mbuffer.getInt();
    }
}
