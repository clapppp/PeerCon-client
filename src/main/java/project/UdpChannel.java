package project;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static project.Main.*;

public class UdpChannel {
    private DatagramChannel channel;
    private final static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final InetSocketAddress serverAddress;
    private Consumer<String> messageConsumer;
    private ScheduledFuture<?> pulseTask;
    private ScheduledFuture<?> p2pTask;
    private volatile boolean connected = false;

    UdpChannel() {
        try {
            this.channel = DatagramChannel.open();
            channel.configureBlocking(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.serverAddress = new InetSocketAddress(SERVER_IP, UDPPORT);
        this.run();
    }

    public void send(String message) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
            channel.send(buffer, serverAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(String message, InetSocketAddress target) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
            channel.send(buffer, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            startPulse();
            listenSignal();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public void startPulse() {
        Runnable task = () -> {
            try {
                send(yourName);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        };
        pulseTask = scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
    }

    private void listenSignal() {
        Thread thread = new Thread(() -> {
            try {
                ByteBuffer listenBuffer = ByteBuffer.allocate(1024);
                while (true) {
                    listenBuffer.clear();
                    channel.receive(listenBuffer);
                    listenBuffer.flip();
                    String message = StandardCharsets.UTF_8.decode(listenBuffer).toString();
                    if (!connected) messageConsumer.accept(message);
                    else System.out.println(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public void setPacketListener(Consumer<String> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    public void startP2P(InetSocketAddress target) {
        //pulse 멈추고
        pulseTask.cancel(true);
        //quit 보내고
        send("quit " + yourName);
        //p2p 연결
        connected = true;
        p2pTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                send(yourName, target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stopP2P() {
        //p2p stop
        p2pTask.cancel(true);
        connected = false;
        //pulse start
        startPulse();
    }
}
