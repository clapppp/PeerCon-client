package project.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Util {
    private static final ByteBuffer listenBuffer = ByteBuffer.allocate(65507);

    public static String receiveUdpToStr(DatagramChannel channel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        channel.receive(buffer);
        buffer.flip();
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

    public static byte[] receiveUdpToByte(DatagramChannel channel) throws IOException {
        listenBuffer.clear();
        channel.receive(listenBuffer);
        System.out.println("udp receive");
        listenBuffer.flip();
        byte[] data = new byte[listenBuffer.remaining()];
        listenBuffer.get(data);
        return data;
    }

    public static InetSocketAddress strToInet(String msg) {
        String[] part = msg.substring(1).split(":", 2);
        return new InetSocketAddress(part[0], Integer.parseInt(part[1]));
    }

    public static byte[] captureScreen(Robot robot, Rectangle screenRect) throws IOException {
        BufferedImage original = robot.createScreenCapture(screenRect);

        int newWidth = 800;
        int newHeight = (int) (original.getHeight() * (800.0 / original.getWidth()));

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.5f);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(resized, null, null), param);
        }

        writer.dispose();
        return baos.toByteArray();
    }

    public static void sendTcp(String request, SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(request.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        socketChannel.write(buffer);
    }

    public static String readTcp(SocketChannel socketChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        socketChannel.read(buffer);
        buffer.flip();
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

    public static boolean readEnd(ByteBuffer buffer) {
        String message = StandardCharsets.UTF_8.decode(buffer).toString();
        int headerEnd = message.indexOf("\r\n\r\n");
        if (headerEnd == -1) return false;

        int headerStartIndex = message.indexOf("Content-Length:");
        if (headerStartIndex == -1) return true;
        int headerEndIndex = message.indexOf("\r\n", headerStartIndex);
        int byteLength = Integer.parseInt(message.substring(headerStartIndex + 15, headerEndIndex).trim());
        String body = message.substring(headerEnd + 4);
        return body.getBytes(StandardCharsets.UTF_8).length == byteLength;
    }
}
