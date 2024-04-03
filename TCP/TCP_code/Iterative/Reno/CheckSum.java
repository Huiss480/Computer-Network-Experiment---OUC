package com.ouc.tcp.test;

import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {
    /*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
    public static short computeChkSum(TCP_PACKET tcpPack) {
        //使用 CRC 循环冗余校验码
        CRC32 crc32 = new CRC32();
        TCP_HEADER header = tcpPack.getTcpH();
        crc32.update(header.getTh_seq());  // update方法使用指定的字节数组更新CRC-32校验和
        crc32.update(header.getTh_ack());

        for (int i = 0; i < tcpPack.getTcpS().getData().length; i++) {
            crc32.update(tcpPack.getTcpS().getData()[i]);
        }
        //getValue用于获取校验和
        return (short) crc32.getValue();
    }

}
