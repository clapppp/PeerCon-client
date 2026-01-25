package project;

import dev.onvoid.webrtc.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

import static project.Main.pulsePort;

public class SignalingClient {
    private final DatagramChannel channel;
    private final InetSocketAddress serverAddress;
    private final ByteBuffer buffer;

    public SignalingClient(DatagramChannel channel, String serverIp) {
        this.channel = channel;
        this.serverAddress = new InetSocketAddress(serverIp, pulsePort);
        this.buffer = ByteBuffer.allocateDirect(65535);
    }

    public void run() {
        System.out.println("Waiting for browser connection (Offer)...");
        try {
            while (true) {
                buffer.clear();
                channel.receive(buffer);
                buffer.flip();

                String message = StandardCharsets.UTF_8.decode(buffer).toString();

                if (message.trim().equals("ok")) continue;

                System.out.println("Received Signal from Server!");
                handleOffer(message);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleOffer(String offerSdp) {
        RTCConfiguration config = new RTCConfiguration();
        RTCIceServer stunServer = new RTCIceServer();
        stunServer.urls.add("stun:stun.l.google.com:19302");
        config.iceServers.add(stunServer);

        RTCPeerConnection pc = Main.factory.createPeerConnection(config, new PeerConnectionObserver() {
            @Override
            public void onDataChannel(RTCDataChannel dataChannel) {
                System.out.println("Data Channel Opened: " + dataChannel.getLabel());
                setupChat(dataChannel);
            }

            @Override
            public void onIceConnectionChange(RTCIceConnectionState state) {
                System.out.println("ICE State: " + state);
            }

            @Override
            public void onIceCandidate(RTCIceCandidate rtcIceCandidate) {
                System.out.println("ICE Candidate: " + rtcIceCandidate);
            }
        });

        RTCSessionDescription offer = new RTCSessionDescription(RTCSdpType.OFFER, offerSdp);

        pc.setRemoteDescription(offer, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                pc.createAnswer(new RTCAnswerOptions(),
                        new CreateSessionDescriptionObserver() {
                            @Override
                            public void onSuccess(RTCSessionDescription answer) {
                                pc.setLocalDescription(answer, new SetSessionDescriptionObserver() {
                                    @Override
                                    public void onSuccess() {
                                        new Thread(() -> {
                                            try {
                                                Thread.sleep(1000);
                                                RTCSessionDescription finalAnswer = pc.getLocalDescription();
                                                sendAnswer(finalAnswer.sdp);
                                            } catch (Exception e) {
                                                System.err.println(e.getMessage());
                                            }
                                        }).start();
                                    }

                                    @Override
                                    public void onFailure(String error) {
                                        System.err.println("SetLocal Fail: " + error);
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String error) {
                                System.err.println("CreateAnswer Fail: " + error);
                            }
                        }
                );
            }

            @Override
            public void onFailure(String error) {
                System.err.println("SetRemote Fail: " + error);
            }
        });
    }

    private void sendAnswer(String sdp) {
        try {
            String json = "SIGNAL|" + Main.yourName + "|" + sdp;

            ByteBuffer buf = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
            channel.send(buf, serverAddress);
            System.out.println("Sent Answer to Server!");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void setupChat(RTCDataChannel dataChannel) {
        dataChannel.registerObserver(new RTCDataChannelObserver() {
            @Override
            public void onMessage(RTCDataChannelBuffer buffer) {
                byte[] bytes = new byte[buffer.data.remaining()];
                buffer.data.get(bytes);
                String msg = new String(bytes, StandardCharsets.UTF_8);
                System.out.println("[Browser]: " + msg);

                try {
                    String reply = "Echo: " + msg;
                    ByteBuffer replyBuf = ByteBuffer.wrap(reply.getBytes(StandardCharsets.UTF_8));
                    dataChannel.send(new RTCDataChannelBuffer(replyBuf, false));
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }

            @Override
            public void onBufferedAmountChange(long amount) {
            }

            @Override
            public void onStateChange() {
            }
        });
    }
}
