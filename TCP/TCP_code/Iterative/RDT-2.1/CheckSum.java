package com.ouc.tcp.test;

import java.util.zip.CRC32;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {

	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		//int checkSum = 0;
		CRC32 checkSum = new CRC32();
		TCP_HEADER header = tcpPack.getTcpH();
		checkSum.update(header.getTh_seq());  // 校验seq字段
		checkSum.update(header.getTh_ack());  // 校验ack字段

		for (int i = 0; i < tcpPack.getTcpS().getData().length; i ++ ) {  // 校验数据字段
			checkSum.update(tcpPack.getTcpS().getData()[i]);
		}
		// getValue返回 CRC-32 值
		return (short) checkSum.getValue();
	}

}
