package project;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static project.Main.pulsePort;

public class Pulse {
    private final DatagramChannel channel;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final InetSocketAddress serverAddress;

    Pulse(DatagramChannel channel, String ip) {
        this.channel = channel;
        this.serverAddress = new InetSocketAddress(ip, pulsePort);
    }

    public void run() {
        try {
            runScheduled();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void runScheduled() {
        Runnable task = () -> {
            try {
                ByteBuffer buffer = ByteBuffer.wrap(Main.yourName.getBytes());
                channel.send(buffer, serverAddress);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
    }
}
