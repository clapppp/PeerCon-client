package project;

import javax.swing.*;
import java.awt.*;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class ClientGui extends JFrame {
    private final TcpChannel tcpChannel;
    private final UdpChannel udpChannel;

    private DefaultListModel<String> listModel; // 단순 String 리스트
    private JList<String> peerList;
    private JButton refreshButton;

    public ClientGui(TcpChannel tcpChannel, UdpChannel udpChannel) {
        this.tcpChannel = tcpChannel;
        this.udpChannel = udpChannel;

        setTitle("P2P Client - " + Main.yourName);
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        refreshButton = new JButton("새로고침");
        add(refreshButton, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        peerList = new JList<>(listModel);

        peerList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selected = peerList.getSelectedValue();
                    if (selected != null) {
                        connectToPeer(selected);
                    }
                }
            }
        });
        add(new JScrollPane(peerList), BorderLayout.CENTER);

        refreshButton.addActionListener(e -> loadList());

        this.udpChannel.setPacketListener(msg -> {
            SwingUtilities.invokeLater(() -> showPopup(msg));
        });

        setVisible(true);
    }

    private void loadList() {
        refreshButton.setEnabled(false);
        CompletableFuture.supplyAsync(() -> tcpChannel.getList())
                .thenAccept(map -> {
                    SwingUtilities.invokeLater(() -> {
                        listModel.clear();
                        // 맵 내용을 "이름 : IP" 문자열로 변환해 추가
                        for (String key : map.keySet()) {
                            if (!key.equals("type") && !key.equals("list")) {
                                listModel.addElement(key + " : " + map.get(key));
                            }
                        }
                        refreshButton.setEnabled(true);
                    });
                });
    }

    private void connectToPeer(String itemString) {
        String[] parts = itemString.split(" : ");
        if (parts.length < 2) return;

        String name = parts[0];

        int choice = JOptionPane.showConfirmDialog(this, name + " 로 연결하시겠습니까?");
        if (choice == JOptionPane.YES_OPTION) {
            CompletableFuture.runAsync(() -> tcpChannel.sendSignal(name));
        }
    }

    private void showPopup(String msg) {
        String[] parts = msg.split("\r\n", 2);
        String[] addrs = parts[1].split(":", 2);
        InetSocketAddress addr = new InetSocketAddress(addrs[0].substring(1), Integer.parseInt(addrs[1]));
        int choice = JOptionPane.showConfirmDialog(this, "UDP 수신 from " + parts[0] + "\n연결할까요?" + addr);
        if (choice == JOptionPane.YES_OPTION) {
            udpChannel.startP2P(addr);
            int stop = JOptionPane.showConfirmDialog(this, "연결 끊기");
            if (stop == JOptionPane.YES_OPTION || stop == JOptionPane.CANCEL_OPTION || stop == JOptionPane.CLOSED_OPTION) {
                udpChannel.stopP2P();
            }
        }
    }


}
