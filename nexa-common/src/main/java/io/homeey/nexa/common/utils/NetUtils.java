package io.homeey.nexa.common.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 *
 * @author jt4mrg@gmail.com
 * @version 0.0.1
 * @since 2025-12-23 23:58
 **/
public final class NetUtils {
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");

    private static volatile String LOCAL_IP;

    public static String getLocalIp() {
        if (LOCAL_IP != null) {
            return LOCAL_IP;
        }
        synchronized (NetUtils.class) {
            if (LOCAL_IP != null) {
                return LOCAL_IP;
            }
            LOCAL_IP = getLocalAddress();
        }
        return LOCAL_IP;
    }

    private static String getLocalAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            if (isValidAddress(localHost)) {
                return localHost.getHostAddress();
            }
        } catch (UnknownHostException e) {
            //do nothing
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (isValidAddress(address)) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            //ignore
        }
        return "127.0.0.1";
    }


    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String ip = address.getHostAddress();
        return ip != null && !ip.startsWith("127.") && IP_PATTERN.matcher(ip).matches();
    }

    public static boolean isValidPort(int port) {
        return port > 0 && port < 65535;
    }
}
