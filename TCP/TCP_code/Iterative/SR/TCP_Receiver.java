package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.TCP_PACKET;

public class TCP_Receiver extends TCP_Receiver_ADT {
    private TCP_PACKET ackPack;  // 回复的 ACK 报文段
    // private int NextSeq = 0;  //记录希望收到的数据包的序号
    private ReceiverWindow ReceiverWin = new ReceiverWindow(this.client);

    /*构造函数*/
    public TCP_Receiver() {
        super();  // 调用超类构造函数
        super.initTCP_Receiver(this);  // 初始化 TCP 接收端
    }

    @Override
    public void rdt_recv(TCP_PACKET recvPack) {
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            // int CurSeq = (recvPack.getTcpH().getTh_seq() - 1) / 100;
            // if (this.NextSeq == CurSeq) {
            //     // 生成ACK报文段
            //     this.tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
            //     this.ackPack = new TCP_PACKET(this.tcpH, this.tcpS, recvPack.getSourceAddr());
            //     this.tcpH.setTh_sum(CheckSum.computeChkSum(this.ackPack));
            //     reply(this.ackPack); // 回复 ACK 报文段
            //     // 将接收到的正确有序的数据插入 data 队列，准备交付
            //     this.dataQueue.add(recvPack.getTcpS().getData());
            //     this.NextSeq++; //更新期望收到的数据包的序号
            // }
            int ack = -1;
            try {
                ack = this.ReceiverWin.recvPacket(recvPack.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            if (ack != -1) {
                this.tcpH.setTh_ack(ack * 100 + 1);
                this.ackPack = new TCP_PACKET(this.tcpH, this.tcpS, recvPack.getSourceAddr());
                this.tcpH.setTh_sum(CheckSum.computeChkSum(this.ackPack));
                reply(this.ackPack);// 回复 ACK 报文段
            }

        }
    }

    @Override
    public void deliver_data() { }

    @Override
    public void reply(TCP_PACKET replyPack) {
        this.tcpH.setTh_eflag((byte) 7);
        // 发送数据报
        this.client.send(replyPack);
    }

}
