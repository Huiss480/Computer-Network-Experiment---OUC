/***************************2.1: ACK/NACK
 **************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.message.*;

public class TCP_Sender extends TCP_Sender_ADT {

    private TCP_PACKET tcpPack;    //待发送数据报
    private volatile int flag = 1;
    private SenderWindow SenderWin = new SenderWindow(client);

    /*构造函数*/
    public TCP_Sender() {
        super();    //调用超类构造函数
        super.initTCP_Sender(this);        //初始化TCP发送端
    }

    @Override
    //可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改
    public void rdt_send(int dataIndex, int[] appData) {
        //生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
        tcpH.setTh_seq(dataIndex * appData.length + 1);//字节流号：
        tcpS.setData(appData);
        tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
        tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));//更新带有checksum的TCP 报文头
        tcpPack.setTcpH(tcpH);
        if (SenderWin.isFull()) {// 发送滑动窗口是否已满
            System.out.println("$$$$$$$$$$SenderWin Full$$$$$$$$$$");
            flag = 0;
        }
        while (flag==0);// 滑动窗口已满，等待ACK报文
        try {
            // 加入包到窗口
            SenderWin.TakePktIn(tcpPack.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        udt_send(tcpPack); // 发送 TCP 数据报
    }

    @Override
    //需要修改
    public void waitACK() { }

    @Override
    //接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为－1；不需要修改
    public void recv(TCP_PACKET recvPack) {
        // 检查校验和
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            System.out.println("Receive ACK Number： "+ recvPack.getTcpH().getTh_ack());
            //调用接收到ACK函数
            SenderWin.recvACK((recvPack.getTcpH().getTh_ack() - 1) / 100);
            if (!SenderWin.isFull()) {
                flag = 1;
            }
        }
    }

    @Override
    public void udt_send(TCP_PACKET stcpPack) {
        //设置错误控制标志
        tcpH.setTh_eflag((byte)7);
        //System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());
        //发送数据报
        client.send(stcpPack);
    }
}
