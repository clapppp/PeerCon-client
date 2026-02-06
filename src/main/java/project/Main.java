package project;

import project.channel.TcpChannel;
import project.channel.UdpChannel;
import project.util.ClientGui;

public class Main {
    public final static int UDPPORT = 8080;
    public final static int TCPPORT = 4000;
    public static String NAME;
    public final static String SERVER_IP = "168.107.4.85";

    public static void main(String[] args) {
        System.out.println("Server Address : " + SERVER_IP);
        NAME = "user" + (int) (Math.random() * 10000);
        System.out.println("Your Name : " + NAME);

        UdpChannel udpChannel = new UdpChannel();
        TcpChannel tcpChannel = new TcpChannel();

        javax.swing.SwingUtilities.invokeLater(() -> {
            new ClientGui(tcpChannel, udpChannel);
        });
    }
}
