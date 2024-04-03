package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.TCP_PACKET;

public class SenderWindow {
    private Client client;
    private int SenderWinSize = 32; //发送方窗口大小
    private int base = 0; //发送窗口左边界
    private int NoPktPtr = 0; //发送窗口中第一个没有放入包的位置
    private TCP_PACKET[] packets = new TCP_PACKET[this.SenderWinSize];
    private UDT_Timer[] timer = new UDT_Timer[this.SenderWinSize]; //为窗口内的每一个数据包都创建一个计时器
    private TaskPacketsRetrans task;

    public SenderWindow(Client client) {
        this.client = client;
    }

    public boolean isFull() {
        return this.SenderWinSize <= this.NoPktPtr;
    }

    public void TakePktIn(TCP_PACKET packet) {
        this.packets[this.NoPktPtr] = packet;
        this.timer[this.NoPktPtr] = new UDT_Timer();
        this.timer[this.NoPktPtr].schedule(new UDT_RetransTask(this.client, packet), 3000, 3000);
        this.NoPktPtr++;
    }

    public void recvACK(int CurSeq) {
        // if (this.base <= CurSeq && CurSeq < this.base + this.SenderWinsize) {  //如果该ACK在发送窗口内
        //     for (int i = 0; CurSeq - this.base + 1 + i < this.SenderWinsize; i++) { //窗口右移==数据包左移
        //         this.packets[i] = this.packets[CurSeq - this.base + 1 + i];
        //         this.packets[CurSeq - this.base + 1 + i] = null;  //移动后留出空闲位置
        //     }
        //     this.NoPktPtr -= CurSeq - this.base + 1; //更新发送窗口中第一个没有放入包的位置
        //     this.base = CurSeq + 1; //更新发送窗口左边界的值
        //     this.timer.cancel(); //停止计时器
        //     if (this.base != this.NoPktPtr) { //如果发送窗口内还有包，则滑动之后还需要将计时器打开
        //         this.timer = new Timer();
        //         this.task = new TaskPacketsRetrans(this.client, this.packets);
        //         this.timer.schedule(this.task, 3000, 3000);
        //     }
        // }
        if (this.base <= CurSeq && CurSeq < this.base + this.SenderWinSize) { //如果该ACK在发送窗口内
            if (this.timer[CurSeq - this.base] == null)  //判断是否为重复的ACK报文，重复则退出
                return;
            this.timer[CurSeq - this.base].cancel();  //不重复就停止该包的计时器
            this.timer[CurSeq - this.base] = null;
            if (CurSeq == this.base) {  //如果收到的ACK是左边界的，则需要滑动发送方窗口
                int LastACKSeq = 0;  //最后一个收到的已经确认的数据包的位置
                while (LastACKSeq + 1 < this.NoPktPtr && this.timer[LastACKSeq + 1] == null)
                    LastACKSeq++;
                for (int i = 0; LastACKSeq + 1 + i < this.SenderWinSize; i++) {
                    this.packets[i] = this.packets[LastACKSeq + 1 + i];
                    this.timer[i] = this.timer[LastACKSeq + 1 + i];
                }
                for (int i = this.SenderWinSize - (LastACKSeq + 1); i < this.SenderWinSize; i++) {
                    this.packets[i] = null;
                    this.timer[i] = null;
                }
                this.base += LastACKSeq + 1;  //滑动到最大的收到的ACK报文的下一个数据包的位置
                this.NoPktPtr -= LastACKSeq + 1;  //更新发送窗口中第一个没有放入包的位置
            }
        }
    }
}
