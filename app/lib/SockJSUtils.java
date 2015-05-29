package lib;

public class SockJSUtils {
    private SockJSUtils() {}

    public static String isWebsocketsEnabled() {
        return System.getProperty("websockets.enabled", "false");
    }
}
