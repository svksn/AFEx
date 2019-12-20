package de.jade_hs.afe.Tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetworkIO {

    static void sendUdpPacket() {

        // TODO:
        // send LSL-Package instead

        Thread thread = new Thread((new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("Localhost: " + InetAddress.getLocalHost());
                    DatagramSocket ds = new DatagramSocket();
                    byte[] buffer = "1".getBytes();
                    DatagramPacket dp = new DatagramPacket(buffer, 0, buffer.length, InetAddress.getLocalHost(), 40007);
                    ds.send(dp);
                } catch (SocketException e) {
                    System.out.println("Send failed. " + e.toString());
                } catch (UnknownHostException e) {
                    System.out.println("Send failed. " + e.toString());
                } catch (IOException e) {
                    System.out.println("Send failed. " + e.toString());
                }
            }
        }));

        thread.start();

    }

}
