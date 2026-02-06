package project.util;

import project.Main;
import project.channel.TcpChannel;
import project.channel.UdpChannel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public class ClientGui extends JFrame {
    private final TcpChannel tcpChannel;
    private final UdpChannel udpChannel;

    // 메인 레이아웃 관리자 (화면 전환용)
    private CardLayout cardLayout;
    private JPanel mainPanel; // 전체를 감싸는 컨테이너

    // [화면 1] 피어 리스트 (메인)
    private JPanel listCard;
    private DefaultListModel<String> listModel;
    private JList<String> peerList;
    private JButton refreshButton;

    // [화면 2] P2P 통신 (화면 공유)
    private JPanel p2pCard;
    private JButton backButton; // "새로고침(뒤로가기)" 역할
    private VideoPanel videoPanel; // 실제 그림이 그려질 패널
    private Timer renderTimer; // 주기적으로 화면 갱신할 타이머

    public ClientGui(TcpChannel tcpChannel, UdpChannel udpChannel) {
        this.tcpChannel = tcpChannel;
        this.udpChannel = udpChannel;

        setTitle("P2P Client - " + Main.NAME);
        setSize(800, 600); // 화면 공유를 위해 사이즈 좀 키움
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    udpChannel.sendPulse("quit " + Main.NAME);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // 1. CardLayout 설정
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 2. 화면들 초기화
        initListCard(); // 메인 리스트 화면 생성
        initP2PCard();  // P2P 통신 화면 생성

        // 3. 카드 추가
        mainPanel.add(listCard, "MainCard");
        mainPanel.add(p2pCard, "P2PCard");

        add(mainPanel);

        // UDP 연결 요청 리스너
        this.udpChannel.setMsgTrigger((msg, target) -> {
            SwingUtilities.invokeLater(() -> showPopup(msg, target));
        });

        setVisible(true);
    }

    private void initListCard() {
        listCard = new JPanel(new BorderLayout());

        refreshButton = new JButton("리스트 새로고침");
        refreshButton.addActionListener(e -> loadList());

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

        listCard.add(refreshButton, BorderLayout.NORTH);
        listCard.add(new JScrollPane(peerList), BorderLayout.CENTER);
    }

    private void initP2PCard() {
        p2pCard = new JPanel(new BorderLayout());

        // 상단: 뒤로가기(새로고침) 버튼
        backButton = new JButton("연결 종료 및 메인으로 (새로고침)");
        backButton.addActionListener(e -> stopP2PAndGoBack());

        // 중앙: 비디오 렌더링 패널
        videoPanel = new VideoPanel();

        p2pCard.add(backButton, BorderLayout.NORTH);
        p2pCard.add(videoPanel, BorderLayout.CENTER);

        // 렌더링 타이머 (약 30 FPS: 33ms)
        // 화면이 활성화될 때만 start() 할 것임
        renderTimer = new Timer(10, e -> videoPanel.repaint());
    }


    // --- 로직 메서드 ---

    // P2P 모드로 전환
    private void switchToP2PMode(InetSocketAddress targetAddr) {
        // 1. 네트워크 연결 시작
        udpChannel.startP2P(targetAddr);

        // 2. 화면 전환
        cardLayout.show(mainPanel, "P2PCard");

        // 3. 렌더링 타이머 시작
        renderTimer.start();
    }

    // 메인으로 복귀
    private void stopP2PAndGoBack() {
        // 1. 렌더링 중지
        renderTimer.stop();

        // 2. 네트워크 연결 종료
        udpChannel.stopP2P();

        // 3. 리스트 갱신하며 복귀
        loadList();
        cardLayout.show(mainPanel, "MainCard");
    }

    private void loadList() {
        refreshButton.setEnabled(false);
        CompletableFuture.supplyAsync(tcpChannel::getList)
                .thenAccept(map -> {
                    SwingUtilities.invokeLater(() -> {
                        listModel.clear();
                        for (String key : map.keySet()) {
                            listModel.addElement(key + " : " + map.get(key));
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

    private void showPopup(String name, InetSocketAddress targetAddr) {
        try {
            int choice = JOptionPane.showConfirmDialog(this, "UDP 연결 요청 " + name + "\n수락하시겠습니까?\n");
            if (choice == JOptionPane.YES_OPTION) {
                switchToP2PMode(targetAddr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- 내부 클래스: 영상 그리는 패널 ---
    private class VideoPanel extends JPanel {
        private BufferedImage currentImage;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // 1. UdpChannel에서 최신 데이터 가져오기 (AtomicReference)
            byte[] data = udpChannel.getLatestFrame();

            if (data != null) {
                try {
                    // 바이트 배열 -> 이미지 변환
                    // (최적화를 위해선 매번 생성하지 말고 재사용하는게 좋지만 일단 기능 구현 위주)
                    currentImage = ImageIO.read(new ByteArrayInputStream(data));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 2. 이미지가 있으면 그리기
            if (currentImage != null) {
                g.drawImage(currentImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                // 대기 화면
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.WHITE);
                g.drawString("Waiting for video signal...", getWidth() / 2 - 50, getHeight() / 2);
            }
        }
    }
}
