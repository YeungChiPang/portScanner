package com.company;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;


public class TcpScanner implements Runnable {
    private static final String scannerResultFilePath = "C:\\check\\";// filePath
    static  String host =null;// "192.168.0.123"; // ip addr
    static int MIN_PORT_NUMBER = 0;// min port
    static int MAX_PORT_NUMBER = 0;// Max port
    static int threadCnt = 0;// thread count
    static int timeout = 1000; // time of time out

    /**
     *
     * @param args
     * @throws InterruptedException
     * @throws IOException
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        Instant instant1  = Instant.now();//program start time
        if (args.length<5){
            System.out.println("Please input params (HOST StratPort EndPort ThreadCount TimeOut):");
            return;
        }else{
            host = args[0];
            MIN_PORT_NUMBER = Integer.parseInt(args[1]);
            MAX_PORT_NUMBER = Integer.parseInt(args[2]);
            threadCnt = Integer.parseInt(args[3]);
            timeout = Integer.parseInt(args[4]);
            System.out.println("HOST:"+host);
            System.out.println("StratPort:"+MIN_PORT_NUMBER);
            System.out.println("EndPort:"+MAX_PORT_NUMBER);
            System.out.println("ThreadCount:"+threadCnt);
            System.out.println("TimeOut:"+timeout);
        }

        int totalPortCnt = MAX_PORT_NUMBER - MIN_PORT_NUMBER;// port process count
        int threadDealCnt = totalPortCnt / threadCnt;// thread process count
        int lastThreadDealCnt = totalPortCnt % threadCnt;// last thread process count
        System.out.println("wait for process Num:" + totalPortCnt);
        System.out.println("process count per thread:" + threadDealCnt);
        System.out.println("reminder is:" + lastThreadDealCnt);
        CountDownLatch countDownLatch = new CountDownLatch(threadCnt);


        int moreLastCnt = MIN_PORT_NUMBER - 1;
        // Thread allocation
        for (int threadIndex = 1; threadIndex <= threadCnt; threadIndex++) {
            int startPort = 0;
            int endPort = 0;
            startPort = moreLastCnt + 1;
            if (threadIndex <= lastThreadDealCnt && 0 != lastThreadDealCnt) {
                endPort = startPort + threadDealCnt;
            } else {
                endPort = startPort + (threadDealCnt - 1);
            }
            moreLastCnt = endPort;
            new TcpScanner(startPort, endPort, countDownLatch).start();
        }
        countDownLatch.await();
        dealFile();
        Instant instant2  = Instant.now();//program end time
        System.out.println("Port Scanner done in seconds :"+ Duration.between(instant1,instant2).getSeconds());
    }


    private static void dealFile() throws IOException {
        System.out.println("-----------del file start-------------");
        File fileDir = new File(scannerResultFilePath);
        File[] files = fileDir.listFiles();
        for (File file : files) {
            long fileLength = file.length();
            if (fileLength == 0) {
                file.delete();
            }
        }


        File resultTxtFile = new File(scannerResultFilePath.concat("/result.txt"));
        if (resultTxtFile.exists()) {
            resultTxtFile.delete();
        }
        resultTxtFile.createNewFile();
        RandomAccessFile randomAccessFile = new RandomAccessFile(resultTxtFile, "rw");
        files = fileDir.listFiles();
        //System.out.println("process file size " + files.length);
        for (File file : files) {
            if (file.getName().contains("result.txt"))
                continue;
           // System.out.println("current deal file " + file.getName());
            randomAccessFile.seek(resultTxtFile.length());
            BufferedReader bufferReader = new BufferedReader(new FileReader(file));
            while (true) {
                String line = bufferReader.readLine();
                if (null == line || "".equals(line.trim())) {
                    break;
                }
                randomAccessFile.write(line.concat("\n").getBytes());
            }
            bufferReader.close();
            file.delete();
        }
        randomAccessFile.close();
        System.out.println("-----------del file end-------------");
    }


    private int begIndex;
    private int endIndex;
    private Thread thread;
    private CountDownLatch countDownLatch;


    public TcpScanner(int begIndex, int endIndex, CountDownLatch countDownLatch) {
        super();
        this.begIndex = begIndex;
        this.endIndex = endIndex;
        this.countDownLatch = countDownLatch;
        try {
            thread = new Thread(this);

            File pathfile = new File(scannerResultFilePath);
            File file = new File(scannerResultFilePath + "result-" + thread.getName() + ".txt");
            if (!pathfile.exists()) {
                file.mkdirs();
            }
            if (file.exists()) {
                file.delete();
            } else {

                file.createNewFile();
            }
            resultFile = new RandomAccessFile(file, "rw");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public TcpScanner start() {
        thread.start();
        return this;
    }


    @Override
    public void run() {
        for (int i = begIndex; i <= endIndex; i++) {
            if (scan(i, timeout)) {
                writeResultToFile(host +":"+ i);
            }
        }
        countDownLatch.countDown();
        close();
    }


    public boolean scan(int port, int timeOut) {
        boolean flag = false;
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(timeOut);
            flag = true;
        } catch (IOException e) {
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
            }
        }
        return flag;
    }


    private RandomAccessFile resultFile = null;

    private void writeResultToFile(String result) {
        try {
            resultFile.writeBytes(result.concat("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void close() {
        if (null != resultFile)
            try {
                resultFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}