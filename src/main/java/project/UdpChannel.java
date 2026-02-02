package project;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static project.Main.*;

public class UdpChannel {
    private DatagramChannel channel;
    private final static ScheduledExecutorService pulseScheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pulseTask;
    private final static ExecutorService p2pScheduler = Executors.newSingleThreadExecutor();
    private Future<?> p2pTask;
    private final InetSocketAddress serverAddress;
    private Consumer<String> messageConsumer;
    private volatile boolean p2pMode = false;
    private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();
    private Robot robot;
    private Rectangle screenRect;

    UdpChannel() {
        try {
            this.channel = DatagramChannel.open();
            channel.configureBlocking(true);

            if (!GraphicsEnvironment.isHeadless()) {
                robot = new Robot();
                screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            }
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

    public void send(byte[] message, InetSocketAddress target) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(message);
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
        pulseTask = pulseScheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
    }

    private void listenSignal() {
        Thread thread = new Thread(() -> {
            try {
                ByteBuffer listenBuffer = ByteBuffer.allocate(65507);
                while (true) {
                    listenBuffer.clear();
                    channel.receive(listenBuffer);
                    listenBuffer.flip();
                    byte[] data = new byte[listenBuffer.remaining()];
                    listenBuffer.get(data);
                    if (!p2pMode) messageConsumer.accept(new String(data, StandardCharsets.UTF_8)); //여기서 gui 로
                    else latestFrame.set(data); //gui에서 여기로 조회
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public byte[] getLatestFrame() {
        return latestFrame.getAndSet(null);
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
        p2pMode = true;
        p2pTask = p2pScheduler.submit(() -> {
            try {
                while (p2pMode && !Thread.currentThread().isInterrupted()) {
                    // 1. 화면 전체 캡처
                    BufferedImage original = robot.createScreenCapture(screenRect);

                    int newWidth = 800;
                    int newHeight = (int) (original.getHeight() * (800.0 / original.getWidth()));

                    BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = resized.createGraphics();
                    g.drawImage(original, 0, 0, newWidth, newHeight, null);
                    g.dispose();

                    // 3. JPG 압축 및 바이트 변환
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(resized, "jpg", baos);
                    byte[] imageBytes = baos.toByteArray();

                    // 4. 전송 (64KB 넘는지 체크)
                    if (imageBytes.length < 65507) {
                        send(imageBytes, target);
                    } else {
                        // 여전히 크면 더 줄여서 재시도하거나 드랍 (여기선 로그만 출력)
                        System.err.println("Frame dropped: too large (" + imageBytes.length + " bytes). Try lowering resolution.");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void stopP2P() {
        //p2p stop
        p2pTask.cancel(true);
        p2pMode = false;
        //pulse start
        startPulse();
    }
}
