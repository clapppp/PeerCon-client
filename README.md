# PeerCon-client

> 📖 [English Version](./README.en.md)

**PeerCon**은 **외부 의존성 없이** 순수 **Java JDK**만으로 구현한 P2P 데스크톱 화면 공유 애플리케이션입니다.  
**Netty**를 공부하던 중 시작한 프로젝트로, Netty와 같은 추상화 레이어 없이 순수 **Java 네트워킹(Java NIO)** 을 깊이 탐구하고 싶었습니다.  
**UDP 홀 펀칭**과 **논블로킹 I/O**를 포함한 저수준 네트워킹의 핵심 메커니즘을 이해하는 것이 목표입니다.

## 스크린샷

[![ScreenGIF](https://github.com/clapppp/PeerCon-client/raw/main/images/peercon_screenvideo.gif)](https://github.com/clapppp/PeerCon-client/blob/main/images/peercon_screenvideo.gif)

다른 PC와 실시간으로 화면이 공유되는 모습입니다.

## 아키텍처

[![Architecture](https://github.com/clapppp/PeerCon-client/raw/main/images/peercon_architecture.png)](https://github.com/clapppp/PeerCon-client/blob/main/images/peercon_architecture.png)

- 서버는 셀렉터(selector)와 워커 스레드(worker-thread) 아키텍처를 사용하여 요청을 처리합니다.
- 클라이언트는 요청 볼륨이 크지 않기 때문에 블로킹 채널을 사용합니다.
- UDP 홀 펀칭으로 연결이 확립된 후, 화면 캡처 및 캡처된 프레임을 피어 간에 전송하는 방식으로 화면 공유를 구현합니다.

## 배운 점

#### [서버](https://github.com/clapppp/PeerCon-Server)

- P2P 직접 통신 환경에서 STUN 서버와 TURN 서버의 역할을 이해했습니다.
- 블로킹 I/O를 언제 사용해야 하는지, 그리고 논블로킹 I/O로 여러 소켓을 관리하기 위해 셀렉터가 필요한 이유를 배웠습니다.
- 대칭형 NAT/라우터 환경에서 P2P 통신의 한계와 TURN 서버의 필요성을 파악했습니다.
- 커스텀 프로토콜을 설계하고 원시 데이터를 직접 다뤘습니다.
- Java NIO를 사용하여 TCP와 UDP 통신을 구현했습니다.

#### 클라이언트

- Java의 `Robot` 클래스를 이용한 화면 캡처 및 압축 기능을 구현했습니다.
- NAT 홀 펀칭을 통해 직접 통신을 구현했습니다.
- 대칭형 NAT 환경에서 발생하는 문제를 분석하고 적절한 해결 방법을 적용했습니다.
- 피어들이 같은 공인 IP 하에 있을 때 사설 IP 기반 통신이 필요한 이유를 이해했습니다.
- 커스텀 프로토콜을 설계하고 원시 데이터를 직접 다뤘습니다.
- Java NIO를 이용한 TCP 및 UDP 통신 기능을 구현했습니다.

## 실행 방법

```bash
git clone https://github.com/clapppp/PeerCon-client.git
cd PeerCon-client/target
java -jar client-1.0.jar
```
