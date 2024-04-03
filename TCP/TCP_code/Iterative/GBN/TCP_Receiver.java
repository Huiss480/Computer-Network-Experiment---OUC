package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class TCP_Receiver extends TCP_Receiver_ADT {

    private TCP_PACKET ackPack;  // 回复的 ACK 报文段
    private int NextSeq = 0;  //记录希望收到的数据包的序号

    public TCP_Receiver() {
        super();  // 调用超类构造函数
        super.initTCP_Receiver(this);  // 初始化 TCP 接收端
    }

    @Override
    public void rdt_recv(TCP_PACKET recvPack) {
        if (CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
            int CurSeq = (recvPack.getTcpH().getTh_seq() - 1) / 100;
            if (this.NextSeq == CurSeq) {
                // 生成ACK报文段
                this.tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
                this.ackPack = new TCP_PACKET(this.tcpH, this.tcpS, recvPack.getSourceAddr());
                this.tcpH.setTh_sum(CheckSum.computeChkSum(this.ackPack));
                reply(this.ackPack); // 回复 ACK 报文段
                // 将接收到的正确有序的数据插入 data 队列，准备交付
                this.dataQueue.add(recvPack.getTcpS().getData());
                this.NextSeq++; //更新期望收到的数据包的序号
            } else {
                // tcpH.setTh_ack(-1);
                // tcpH.setTh_ack(front_pkt_seq * 100 + 1);
                tcpH.setTh_ack((NextSeq - 1) * 100 + 1);  // 设置确认号为已确认的最大序号
                ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
                tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
                reply(ackPack); //回复ACK报文段
            }
        }
        // 交付数据（每 20 组数据交付一次）
        if (this.dataQueue.size() == 20)
            deliver_data();
    }

    @Override
    public void deliver_data() {
        // 检查 this.dataQueue，将数据写入文件
        try {
            File file = new File("recvData.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

            while (!this.dataQueue.isEmpty()) {
                int[] data = this.dataQueue.poll();

                // 将数据写入文件
                for (int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }

                writer.flush();  // 清空输出缓存
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reply(TCP_PACKET replyPack) {
        // 设置错误控制标志
        this.tcpH.setTh_eflag((byte)7);
        // 发送数据报
        this.client.send(replyPack);
    }

}
