package io.homeey.nexa.common.constants;

/**
 *
 * @author jt4mrg@gmail.com
 * @version 0.0.1
 * @since 2025-12-23 23:44
 **/
public interface Constants {
    String DEFAULT_PROTOCOL = "nexa";
    String DEFAULT_HOST = "0.0.0.0";
    int DEFAULT_PORT = 12345;

    int DEFAULT_TIMEOUT = 3000;
    int DEFAULT_CONNECT_TIMEOUT = 5000;

    int DEFAULT_IO_THREADS = Math.min(32, Runtime.getRuntime().availableProcessors() + 1);

    String DEFAULT_SERIALIZATION = "jdk";


    String TIMEOUT_KEY = "timeout";
    String VERSION_KEY = "version";
    String SERIALIZATION_KEY = "serialization";
    String GROUP_KEY = "group";
    String INTERFACE_KEY = "interface";
    String SIDE_KEY = "side";

    String SIDE_PROVIDER = "provider";
    String SIDE_CONSUMER = "consumer";

    short MAGIC_NUMBER = (short) 0xdabb;
    byte VERSION = 1;
}
