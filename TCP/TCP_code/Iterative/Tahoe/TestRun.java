package com.ouc.tcp.test;

import com.ouc.tcp.app.SystemStart;

import java.io.*;

public class TestRun {
    public static void main(String[] args) throws InterruptedException, IOException {
        File f=new File("copy.txt");
        f.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(f);
        PrintStream printStream = new PrintStream(fileOutputStream);
        System.setOut(printStream);
        SystemStart.main(null);
    }
}
