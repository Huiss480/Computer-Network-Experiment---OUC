package com.ouc.tcp.test;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;

import java.util.Timer;

public class SenderWindow {
    private Client client;
    private int SenderWinsize = 32;  //发送方窗口大小
    private int base = 0;  //发送窗口左边界
    private int NoPktPtr = 0;  //发送窗口中第一个没有放入包的位置
    private TCP_PACKET[] packets = new TCP_PACKET[this.SenderWinsize];
    private Timer timer;
    private TaskPacketsRetrans task;

    public SenderWindow(Client client) {
        this.client = client;
    }

    public boolean isFull() {
        return this.SenderWinsize <= this.NoPktPtr;
    }

    public void TakePktIn(TCP_PACKET packet) {
        this.packets[this.NoPktPtr] = packet;
        if (this.base == this.NoPktPtr) {
            this.timer = new Timer();
            this.task = new TaskPacketsRetrans(this.client, this.packets);
            this.timer.schedule(this.task, 3000, 3000);
        }
        this.NoPktPtr++;
    }

    public void recvACK(int CurSeq) {
        if (this.base <= CurSeq && CurSeq < this.base + this.SenderWinsize) {  //如果该ACK在发送窗口内
            for (int i = 0; CurSeq - this.base + 1 + i < this.SenderWinsize; i++) { //窗口右移==数据包左移
                this.packets[i] = this.packets[CurSeq - this.base + 1 + i];
                this.packets[CurSeq - this.base + 1 + i] = null;  //移动后留出空闲位置
            }
            this.NoPktPtr -= CurSeq - this.base + 1; //更新发送窗口中第一个没有放入包的位置
            this.base = CurSeq + 1; //更新发送窗口左边界的值
            this.timer.cancel(); //停止计时器
            if (this.base != this.NoPktPtr) { //如果发送窗口内还有包，则滑动之后还需要将计时器打开
                this.timer = new Timer();
                this.task = new TaskPacketsRetrans(this.client, this.packets);
                this.timer.schedule(this.task, 3000, 3000);
            }
        }
    }
}



