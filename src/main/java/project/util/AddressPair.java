package project.util;

import java.net.InetSocketAddress;

public record AddressPair(InetSocketAddress publicAddress, InetSocketAddress privateAddress, boolean isSymmetric) {
}
