package project.channel;

import project.util.RawRequest;
import project.util.Util;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;

import static project.Main.*;

public class TcpChannel {
    private final InetSocketAddress serverAddress;

    public TcpChannel() {
        serverAddress = new InetSocketAddress(SERVER_IP, TCPPORT);
    }

    public Map<String, String> getList() {
        String request = RawRequest.makeRaw(Map.of("type", "list"));
        RawRequest raw;
        try (SocketChannel socketChannel = SocketChannel.open(serverAddress)) {
            Util.sendTcp(request, socketChannel);
            raw = new RawRequest(Util.readTcp(socketChannel));
            return raw.toStringToMap(raw.getBody().get("list"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendSignal(String target) {
        String request = RawRequest.makeRaw(Map.of("type", "signal", "target", target, "sender", NAME));
        try (SocketChannel socketChannel = SocketChannel.open(serverAddress)) {
            Util.sendTcp(request, socketChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
