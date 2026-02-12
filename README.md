# PeerCon

## Overview
**PeerCon** is a peer-to-peer desktop sharing application built entirely with **Java JDK**, utilizing **no external dependencies**.

I started this project while studying **Netty**. I wanted to explore **pure Java networking (Java NIO)** without relying on abstraction layers like Netty. I aim to understand the core mechanisms of low-level networking, including **UDP hole punching** and **non-blocking I/O**.

## Architecture
This project consists of two main components: **Server** and **Client**.

### Server ([Link to Repository](https://github.com/clapppp/PeerCon-server))
The server acts as a lightweight **STUN server** and signaling coordinator.
- **NAT Type Detection:** Determines if a client is behind a **Symmetric NAT** or a Cone NAT.
- **Connection Maintenance:** Manages the active client list by receiving periodic **UDP pulses**.
- **Peer Discovery:** When a client requests a P2P connection, the server provides the target client's public IP and port to facilitate direct communication.

### Client
The client handles peer-to-peer connection establishment and screen streaming.
- **Heartbeat Mechanism:** Sends periodic UDP pulses to the server containing metadata (Username, Public/Private IP, NAT Type) to keep the NAT mapping alive.
- **P2P Establishment:** Once a target peer is selected, it retrieves the target's IP from the server and initiates **UDP Hole Punching**.
- **Screen Sharing:** Captures the desktop screen using `java.awt.Robot` and streams it to the connected peer.

## Challenges

### 1. Symmetric NAT Traversal
- **Issue:** Under a **Symmetric NAT**, standard UDP hole punching is unavailable.
- **Solution:** To solve this, a **Relay Server (TURN)** implementation is required to bridge traffic when direct P2P fails.

### 2. High Resolution & Packet Fragmentation
- **Issue:** For higher resolution screen sharing, simple image compression is not enough. Since the application currently sends raw compressed data over UDP, frames larger than the **MTU (Maximum Transmission Unit)** cause packet loss.
- **Solution:** A **Custom Application-Layer Protocol** is needed to handle:
    - **Packet Fragmentation:** Splitting large image frames into smaller UDP packets.
    - **Reassembly:** Reordering and rebuilding frames at the receiver end to ensure smooth rendering.
