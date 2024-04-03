package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

public class SenderWindow {
    public Client client;  //客户端
    private volatile int ssthresh = 16;  //门限值
    public int cwnd = 1;  //拥塞窗口
    private int FrontACKSeq = -1; //上一次收到的ACK的包的seq
    private int RepeatACKnum = 0; //重复的Ack数，用于快重传判断
    private int CAnum = 0; // 进入拥塞避免状态时收到的ACK数，记录一个RTT收到ACK的进度
    private Hashtable<Integer, TCP_PACKET> packets = new Hashtable<>(); // 存储窗口内的数据包
    private Hashtable<Integer, UDT_Timer> timer = new Hashtable<>(); // 存储每个数据包的计时器

    public SenderWindow(Client client) {
        this.client = client;
    }

    /*判断窗口是否已满*/
    public boolean isFull() {
        return cwnd <= packets.size();
    }

    // 加入包到窗口
    public void TakePktIn(TCP_PACKET packet) {
        int CurSeq = (packet.getTcpH().getTh_seq() - 1) / 100;
        timer.put(CurSeq, new UDT_Timer());
        timer.get(CurSeq).schedule(new TahoeRetran(client, packet), 3000, 3000);
        packets.put(CurSeq, packet);
    }

    // 接收到ACK
    public void recvACK(int CurSeq) {
        // // if (this.base <= CurSeq && CurSeq < this.base + this.SenderWinsize) {  //如果该ACK在发送窗口内
        // //     for (int i = 0; CurSeq - this.base + 1 + i < this.SenderWinsize; i++) { //窗口右移==数据包左移
        // //         this.packets[i] = this.packets[CurSeq - this.base + 1 + i];
        // //         this.packets[CurSeq - this.base + 1 + i] = null;  //移动后留出空闲位置
        // //     }
        // //     this.NoPktPtr -= CurSeq - this.base + 1; //更新发送窗口中第一个没有放入包的位置
        // //     this.base = CurSeq + 1; //更新发送窗口左边界的值
        // //     this.timer.cancel(); //停止计时器
        // //     if (this.base != this.NoPktPtr) { //如果发送窗口内还有包，则滑动之后还需要将计时器打开
        // //         this.timer = new Timer();
        // //         this.task = new TaskPacketsRetrans(this.client, this.packets);
        // //         this.timer.schedule(this.task, 3000, 3000);
        // //     }
        // // }
        // if (this.base <= CurSeq && CurSeq < this.base + this.SenderWinSize) { //如果该ACK在发送窗口内
        //     if (this.timer[CurSeq - this.base] == null)  //判断是否为重复的ACK报文，重复则退出
        //         return;
        //     this.timer[CurSeq - this.base].cancel();  //不重复就停止该包的计时器
        //     this.timer[CurSeq - this.base] = null;
        //     if (CurSeq == this.base) {  //如果收到的ACK是左边界的，则需要滑动发送方窗口
        //         int LastACKSeq = 0;  //最后一个收到的已经确认的数据包的位置
        //         while (LastACKSeq + 1 < this.NoPktPtr && this.timer[LastACKSeq + 1] == null)
        //             LastACKSeq++;
        //         for (int i = 0; LastACKSeq + 1 + i < this.SenderWinSize; i++) {
        //             this.packets[i] = this.packets[LastACKSeq + 1 + i];
        //             this.timer[i] = this.timer[LastACKSeq + 1 + i];
        //         }
        //         for (int i = this.SenderWinSize - (LastACKSeq + 1); i < this.SenderWinSize; i++) {
        //             this.packets[i] = null;
        //             this.timer[i] = null;
        //         }
        //         this.base += LastACKSeq + 1;  //滑动到最大的收到的ACK报文的下一个数据包的位置
        //         this.NoPktPtr -= LastACKSeq + 1;  //更新发送窗口中第一个没有放入包的位置
        //     }
        // }
        if (CurSeq != FrontACKSeq) { //新到来的ACK包
            for (int i = FrontACKSeq + 1; i <= CurSeq; i++) {
                packets.remove(i);
                if (timer.containsKey(i)) {
                    timer.get(i).cancel();
                    timer.remove(i);
                }
            }
            FrontACKSeq = CurSeq;
            RepeatACKnum = 0;
            if (cwnd < ssthresh) { //慢开始算法
                System.out.println("----------执行慢开始算法----------");
                System.out.println("根据慢开始算法，每收到一个ACK就将cwnd增加1，因此cwnd由" + cwnd + "增长为" + (cwnd + 1));
                System.out.println();
                cwnd++;
            } else { //拥塞避免
                CAnum++;
                System.out.println("----------执行拥塞避免算法----------");
                System.out.println("根据拥塞避免算法，每经过一个RTT才把cwnd增加1，此时cwnd为" + cwnd + "，拥塞避免RTT的进度为" + CAnum);
                System.out.println("只有当RTT进度达到" + cwnd + "时才把cwnd增加1\n");
                if (CAnum >= cwnd) {  // 收到一个RTT内ACK数量超过 cwnd
                    CAnum -= cwnd;  // 重置RTT进度
                    System.out.println("此时RTT进度达到cwnd，cwnd需要增加1，因此cwnd由" + cwnd + "增长为" + (cwnd + 1));
                    System.out.println();
                    cwnd++;
                }
            }
        } else { //重复的ACK包
            RepeatACKnum++;
            if(RepeatACKnum >= 3){ //重复收到3次
                TCP_PACKET packet = packets.get(CurSeq + 1);
                if (packet != null) {
                    System.out.println("----------执行重传----------");
                    client.send(packet);
                    timer.get(CurSeq + 1).cancel();
                    timer.put(CurSeq + 1, new UDT_Timer());
                    timer.get(CurSeq + 1).schedule(new TahoeRetran(client, packet), 3000, 3000);
                }
                System.out.println("根据快重传算法，应该将cwnd由" + cwnd + "变为" + 1);
                System.out.println("ssthresh应该变为拥塞窗口的一半，因此ssthresh由" + ssthresh + "变为" + Math.max(cwnd / 2, 2));
                System.out.println();
                ssthresh = Math.max(cwnd / 2, 2);
                cwnd = 1;
            }
        }
    }


    class TahoeRetran extends UDT_RetransTask {
        int CurSeq;
        private TCP_PACKET packet;
        public TahoeRetran(Client client, TCP_PACKET packet) {
            super(client, packet);
            CurSeq = packet.getTcpH().getTh_seq();
            this.packet = packet;
        }
        @Override
        public void run() {
            System.out.println("！！！！！！！！！！超时重传！！！！！！！！！！");
            System.out.println("超时重传应当将ssthresh设定为拥塞窗口的一半，因此ssthresh由" + ssthresh + "变为" + Math.max(cwnd / 2, 2));
            System.out.println("\n将cwnd变为1\n");
            ssthresh = Math.max(cwnd / 2, 2);
            cwnd = 1;
            super.run();
        }
    }

}


