package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class ReceiverWindow {
    private Client client;
    private int ReceiverWinSize = 32;
    private int base = 0;
    private TCP_PACKET[] packets = new TCP_PACKET[this.ReceiverWinSize];
    Queue<int[]> dataQueue = new LinkedBlockingQueue<>();
    private int counts = 0;

    public ReceiverWindow(Client client) {
        this.client = client;
    }

    public int recvPacket(TCP_PACKET packet) {
        int CueSeq = (packet.getTcpH().getTh_seq() - 1) / 100; //得到当前包的序号
        if (CueSeq < this.base) {  //该包的序号不在接受窗口内
            int left = this.base - this.ReceiverWinSize;
            int right = this.base - 1;
            if (left <= 0)
                left = 1;
            if (left <= CueSeq && CueSeq <= right)
                return CueSeq;
        } else if (this.base <= CueSeq && CueSeq < this.base + this.ReceiverWinSize) { //该包的序号在接收窗口内
            this.packets[CueSeq - this.base] = packet;  //放入接收窗口
            if (CueSeq == this.base)  //当前包的序号位于左边界
                this.slide();
            return CueSeq;
        }
        return -1;
    }

    private void slide() {  //滑动方法
        int maxPkt = 0;
        while (maxPkt + 1 < this.ReceiverWinSize && this.packets[maxPkt + 1] != null)
            maxPkt++;  //循环迭代最大的数据包

        for (int i = 0; i < maxPkt + 1; i++) //将接收到的包加入数据队列
            this.dataQueue.add(this.packets[i].getTcpS().getData());

        for (int i = 0; maxPkt + 1 + i < this.ReceiverWinSize; i++) //左移数据包，类似GBN中的发送方
            this.packets[i] = this.packets[maxPkt + 1 + i];

        for (int i = this.ReceiverWinSize - (maxPkt + 1); i < this.ReceiverWinSize; i++)
            this.packets[i] = null;  //移动后原位置上的数据包位置置为空

        this.base += maxPkt + 1;  //更新左边界
        if (this.dataQueue.size() >= 20 || this.base == 1000) {
            this.deliver_data();
        }
    }

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

}
