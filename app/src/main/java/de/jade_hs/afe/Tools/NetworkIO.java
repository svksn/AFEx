package de.jade_hs.afe.Tools;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class NetworkIO {

    public static void sendUdpPacket() {

        Thread thread = new Thread((new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Localhost: " + InetAddress.getLocalHost());
                    DatagramSocket ds = new DatagramSocket();
                    byte[] buffer = "1".getBytes();
                    DatagramPacket dp = new DatagramPacket(buffer, 0, buffer.length, InetAddress.getLocalHost(), 40007);
                    ds.send(dp);
                } catch (Exception e) {
                    System.out.println("Send failed. " + e.toString());
                }
            }
        }));

        thread.start();

    }

}
