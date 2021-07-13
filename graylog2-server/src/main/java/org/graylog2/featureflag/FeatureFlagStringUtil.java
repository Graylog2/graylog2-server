package org.graylog2.featureflag;

import java.util.Locale;

class FeatureFlagStringUtil {

    static String toUpperCase(String s) {
        return s.toUpperCase(Locale.ROOT);
    }

    static boolean startsWithIgnoreCase(String text, String start) {
        return toUpperCase(text).startsWith(toUpperCase(start));
    }
}
