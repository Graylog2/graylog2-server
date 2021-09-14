package org.graylog2.shared.utilities;

import java.util.Locale;

public final class StringUtils {

    private StringUtils() { }

    public static String f(String format, Object... args) {
        return String.format(Locale.ENGLISH, format, args);
    }
}
