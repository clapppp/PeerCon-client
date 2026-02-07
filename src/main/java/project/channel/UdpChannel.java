package project.channel;

import project.util.Util;

import java.awt.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static project.Main.*;

public class UdpChannel {
    private DatagramChannel channel;
    private boolean isSymmetric;
    private InetSocketAddress privateAddress;
    private InetSocketAddress targetAddress;
    private final InetSocketAddress serverAddress;
    private final static ScheduledExecutorService pulseScheduler = Executors.newSingleThreadScheduledExecutor();
    private final static ExecutorService p2pScheduler = Executors.newSingleThreadExecutor();
    private BiConsumer<String, InetSocketAddress> msgTrigger;
    private volatile boolean p2pMode = false;
    private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();
    private Robot robot;
    private Rectangle screenRect;

    public UdpChannel() {
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(true);
            if (!GraphicsEnvironment.isHeadless()) {
                robot = new Robot();
                screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverAddress = new InetSocketAddress(SERVER_IP, UDPPORT);
        setPrivateAddr();
        setSymmetric();
        startPulse();
        listenSignal();
    }

    private void setPrivateAddr() {
        try {
            channel.connect(serverAddress);
            privateAddress = (InetSocketAddress) channel.getLocalAddress();
            channel.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setSymmetric() {
        try {
            sendSTUN(serverAddress);
            String ip1 = Util.receiveUdpToStr(channel);
            System.out.println(ip1);
            sendSTUN(new InetSocketAddress(SERVER_IP, UDPPORT + 1));
            String ip2 = Util.receiveUdpToStr(channel);
            System.out.println(ip2);
            isSymmetric = !ip1.equals(ip2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPulse(String message) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
            channel.send(buffer, serverAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendP2P(byte[] message, InetSocketAddress target) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message);
            channel.send(buffer, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSTUN(InetSocketAddress target) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap("STUN".getBytes(StandardCharsets.UTF_8));
            channel.send(buffer, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startPulse() {
        pulseScheduler.scheduleAtFixedRate(() -> {
            try {
                if (!p2pMode) sendPulse(NAME + " " + privateAddress + " " + isSymmetric);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void listenSignal() {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    byte[] data = Util.receiveUdpToByte(channel);
                    if (!p2pMode && data.length < 10000) {
                        String name = handleTarget(new String(data, StandardCharsets.UTF_8));
                        msgTrigger.accept(name, targetAddress);
                        p2pMode = true;
                    } else {
                        if (Arrays.equals(data, "end".getBytes(StandardCharsets.UTF_8))) {
                            p2pMode = false;
                            System.out.println("OPPONENT ENDED P2P");
                            continue;
                        }
                        latestFrame.set(data);
                        System.out.println("set latestFrame");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private String handleTarget(String msg) {
        String[] line = msg.split("\r\n", 2);
        String[] me = line[0].split(" ", 3);
        String[] you = line[1].split(" ", 3);
        if (me[1].split(":")[0].equals(you[1].split(":")[0])) {
            targetAddress = Util.strToInet(you[2]);
        } else {
            targetAddress = Util.strToInet(you[1]);
        }
        return you[0];
    }

    public byte[] getLatestFrame() {
        return latestFrame.getAndSet(null);
    }

    public void setMsgTrigger(BiConsumer<String, InetSocketAddress> msgTrigger) {
        this.msgTrigger = msgTrigger;
    }

    public void startP2P(InetSocketAddress target) {
        sendPulse("quit " + NAME);

        p2pScheduler.submit(() -> {
            try {
                while (p2pMode && !Thread.currentThread().isInterrupted()) {
                    byte[] imageBytes = Util.captureScreen(robot, screenRect);
                    if (imageBytes.length < 65507) {
                        sendP2P(imageBytes, target);
                    } else {
                        System.err.println("Frame dropped: too large (" + imageBytes.length + " bytes).");
                    }
                    Thread.sleep(33);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void stopP2P() {
        try {
            channel.send(ByteBuffer.wrap("end".getBytes(StandardCharsets.UTF_8)), targetAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
        p2pMode = false;
    }
}
