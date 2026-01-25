package project;

import dev.onvoid.webrtc.PeerConnectionFactory;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.util.Scanner;

public class Main {
    public static int pulsePort = 8080;
    public static final PeerConnectionFactory factory = new PeerConnectionFactory();
    public static String yourName;

    public static void main(String[] args) throws IOException {
        System.out.println("Input Server Address : ");
        Scanner sc = new Scanner(System.in);
        String serverAddress = sc.nextLine();
        if (serverAddress.isEmpty()) serverAddress = "127.0.0.1";

        System.out.println("Input Your Name : ");
        yourName = sc.nextLine();
        if (yourName.isEmpty()) yourName = "user" + (int) (Math.random() * 100);

        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(true);

        new Pulse(channel, serverAddress).run();
        new SignalingClient(channel, serverAddress).run();
    }
}


