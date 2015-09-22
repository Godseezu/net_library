/* Copyright (c) 2008, Nathan Sweet
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package lc.com.net_library.tcpudp;


import lc.com.net_library.tcpudp.util.DataUtils;

/**
 * 临时数据封装
 *
 * @author Lin
 */
public interface FrameworkMessage {

    byte[] getByte();

    /**
     * Internal message to give the client the server assigned connection ID.
     */
    static public class RegisterTCP implements FrameworkMessage {
        static public final byte ONE = 0x00;
        static public final byte TWO = 0x00;
        static public final byte THREE = 0x00;
        public int connectionID=-1;

        @Override
        public byte[] getByte() {
            byte [] conID= DataUtils.int2byte(connectionID);
            byte[] data = new byte[3+4];
            data[0]=ONE;
            data[1]=TWO;
            data[2]=THREE;
            data[3]=conID[0];
            data[4]=conID[1];
            data[5]=conID[2];
            data[6]=conID[3];

            return data;
        }

        static public boolean isRegisterTCP(byte[] data) {
            if (data.length >= 3) {
                if (data[0] == ONE && data[1] == TWO && data[2] == THREE) {
                    return true;
                }
            }
            return false;
        }
    }


    /**
     * Internal message to give the server the client's UDP port.
     */
    static public class RegisterUDP implements FrameworkMessage {
        static public final byte ONE = 0x00;
        static public final byte TWO = 0x01;
        static public final byte THREE = 0x00;

        public int connectionID=-1;

        @Override
        public byte[] getByte() {
            byte [] conID= DataUtils.int2byte(connectionID);
            byte[] data = new byte[3+4];
            data[0]=ONE;
            data[1]=TWO;
            data[2]=THREE;
            data[3]=conID[0];
            data[4]=conID[1];
            data[5]=conID[2];
            data[6]=conID[3];
            return data;
        }

        static public boolean isRegisterUDP(byte[] data) {
            if (data.length >= 3) {
                if (data[0] == ONE && data[1] == TWO && data[2] == THREE) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Internal message to keep connections alive.
     */
    static public class KeepAlive implements FrameworkMessage {
        static public final byte ONE = 0x00;
        static public final byte TWO = 0x02;
        static public final byte THREE = 0x00;


        @Override
        public byte[] getByte() {
            byte[] data = {ONE, TWO, THREE};
            return data;
        }

        static public boolean isKeepAlive(byte[] data) {
            if (data.length >= 3) {
                if (data[0] == ONE && data[1] == TWO && data[2] == THREE) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Internal message to discover running servers.
     */
    static public class DiscoverHost implements FrameworkMessage {
        static public final byte ONE = 0x00;
        static public final byte TWO = 0x03;
        static public final byte THREE = 0x00;


        @Override
        public byte[] getByte() {
            byte[] data = {ONE, TWO, THREE};
            return data;
        }

        static public boolean isDiscoverHost(byte[] data) {
            if (data.length >= 3) {
                if (data[0] == ONE && data[1] == TWO && data[2] == THREE) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Internal message to determine round trip time.
     */
    static public class Ping implements FrameworkMessage {
        static public final byte ONE = 0x00;
        static public final byte TWO = 0x04;
        static public final byte THREE = 0x00;


        @Override
        public byte[] getByte() {
            byte[] data = {ONE, TWO, THREE};
            return data;
        }

        static public boolean isPing(byte[] data) {
            if (data.length >= 3) {
                if (data[0] == ONE && data[1] == TWO && data[2] == THREE) {
                    return true;
                }
            }
            return false;
        }
    }


}
