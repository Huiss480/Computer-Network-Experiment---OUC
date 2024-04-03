/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

public class TCP_Receiver extends TCP_Receiver_ADT {

    private TCP_PACKET ackPack;	//回复的ACK报文段
    private int NextSeq = 0;  // 用于记录期望收到的seq
    // private ReceiverWindow ReceiverWin = new ReceiverWindow(this.client);
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>(); // 用于缓存失序分组


    /*构造函数*/
    public TCP_Receiver() {
        super();    //调用超类构造函数
        super.initTCP_Receiver(this);    //初始化TCP接收端
    }

    @Override
    //接收到数据报：检查校验和，设置回复的ACK报文段
    public void rdt_recv(TCP_PACKET recvPack) {
        if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
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
            // int ack = -1;
            // try {
            //     ack = this.ReceiverWin.recvPacket(recvPack.clone());
            // } catch (CloneNotSupportedException e) {
            //     e.printStackTrace();
            // }
            // if (ack != -1) {
            //     this.tcpH.setTh_ack(ack * 100 + 1);
            //     this.ackPack = new TCP_PACKET(this.tcpH, this.tcpS, recvPack.getSourceAddr());
            //     this.tcpH.setTh_sum(CheckSum.computeChkSum(this.ackPack));
            //     reply(this.ackPack);// 回复 ACK 报文段
            // }
            int CurSeq = (recvPack.getTcpH().getTh_seq() - 1) / 100;
            if (NextSeq == CurSeq) {
                dataQueue.add(recvPack.getTcpS().getData());
                NextSeq++;
                // 将窗口中CurSeq之后连续包的数据加入数据队列
                while(packets.containsKey(NextSeq)){
                    dataQueue.add(packets.get(NextSeq).getTcpS().getData());
                    packets.remove(NextSeq); //从接收方窗口中移出
                    NextSeq++;
                }
                //每20组数据交付一次
                if(dataQueue.size() >= 20)
                    deliver_data();
            } else {  // 无序
                if (!packets.containsKey(CurSeq) && CurSeq > NextSeq)
                    packets.put(CurSeq, recvPack); // 加入接收方窗口
            }
        }
        tcpH.setTh_ack((NextSeq - 1) * 100 + 1);//生成ACK报文段
        ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
        tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
        reply(ackPack);
    }

    @Override
    //交付数据（将数据写入文件）；不需要修改
    public void deliver_data() {
        //检查dataQueue，将数据写入文件
        File fw = new File("recvData.txt");
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(fw, true));
            //循环检查data队列中是否有新交付数据
            while(!dataQueue.isEmpty()) {
                int[] data = dataQueue.poll();
                //将数据写入文件
                for(int i = 0; i < data.length; i++) {
                    writer.write(data[i] + "\n");
                }
                writer.flush();		//清空输出缓存
            }
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    //回复ACK报文段
    public void reply(TCP_PACKET replyPack) {
        //设置错误控制标志
        tcpH.setTh_eflag((byte) 7);
        //发送数据报
        client.send(replyPack);
    }
}
