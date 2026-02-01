package project;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static project.Main.*;

public class TcpChannel {
    private final InetSocketAddress serverAddress;
    private final ByteBuffer buffer;

    public TcpChannel() {
        serverAddress = new InetSocketAddress(SERVER_IP, TCPPORT);
        buffer = ByteBuffer.allocateDirect(65535);
    }

    public Map<String, String> getList() {
        String request = RawRequest.makeRaw(Map.of("type", "list"));
        buffer.clear();
        buffer.put(request.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        try (SocketChannel socketChannel = SocketChannel.open(serverAddress)) {
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }

            buffer.clear();
            while (socketChannel.read(buffer) > 0) {
                ByteBuffer readOnly = buffer.duplicate().flip();
                if (readEnd(readOnly)) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        buffer.flip();
        RawRequest raw = new RawRequest(StandardCharsets.UTF_8.decode(buffer).toString());
        return raw.toStringToMap(raw.getBody().get("list"));
    }

    public void sendSignal(String target) {
        String request = RawRequest.makeRaw(Map.of("type", "signal", "target", target, "sender", yourName));
        buffer.clear();
        buffer.put(request.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        try (SocketChannel socketChannel = SocketChannel.open(serverAddress)) {
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean readEnd(ByteBuffer buffer) {
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
