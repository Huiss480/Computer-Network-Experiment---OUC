package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.message.TCP_PACKET;

public class TCP_Sender extends TCP_Sender_ADT {
    private TCP_PACKET tcpPack;  // 待发送的 TCP 数据报
    private volatile int flag = 1;
    private SenderWindow SenderWin = new SenderWindow(this.client);

    public TCP_Sender() {
        super();  // 调用超类构造函数
        super.initTCP_Sender(this);  // 初始化 TCP 发送端
    }

    @Override
    public void rdt_send(int dataIndex, int[] appData) {
        // 生成 TCP 数据报（设置序号、数据字段、校验和)，注意打包的顺序
        this.tcpH.setTh_seq(dataIndex * appData.length + 1);  // 包序号设置为字节流号
        this.tcpS.setData(appData);
        this.tcpPack = new TCP_PACKET(this.tcpH, this.tcpS, this.destinAddr);
        this.tcpH.setTh_sum(CheckSum.computeChkSum(this.tcpPack));
        this.tcpPack.setTcpH(this.tcpH);
        if (this.SenderWin.isFull()) {
            System.out.println("$$$$$$$$$$SenderWin Full$$$$$$$$$$");
            this.flag = 0;
        }
        while (this.flag == 0); //发送窗口已经放满，等待ACK报文
        try {
            this.SenderWin.TakePktIn(this.tcpPack.clone());
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        udt_send(this.tcpPack); // 发送 TCP 数据报
    }

    @Override
    public void waitACK() { }

    @Override
    public void recv(TCP_PACKET recvPack) {
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            System.out.println("Receive ACK Number: " + recvPack.getTcpH().getTh_ack());
            this.SenderWin.recvACK((recvPack.getTcpH().getTh_ack() - 1) / 100);
            if (!this.SenderWin.isFull()) {
                this.flag = 1;
            }
        }
    }

    @Override
    public void udt_send(TCP_PACKET stcpPack) {
        // 设置错误控制标志
        this.tcpH.setTh_eflag((byte) 7);
        // 发送数据报
        this.client.send(stcpPack);
    }
}
