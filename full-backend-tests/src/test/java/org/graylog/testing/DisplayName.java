package org.graylog.testing;

public class DisplayName {
    static String canonize(String name) {
        return name.replaceAll("_", " ");
    }
}
