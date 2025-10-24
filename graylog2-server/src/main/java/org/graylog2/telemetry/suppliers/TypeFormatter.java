package org.graylog2.telemetry.suppliers;

import java.util.List;
import java.util.Locale;

public class TypeFormatter {
    private static final List<String> PACKAGE_PREFIXES = List.of(
            "org.graylog2.inputs",
            "org.graylog.plugins",
            "org.graylog.aws.inputs",
            "org.graylog.enterprise.integrations"
    );

    public static String format(String type) {
        for (String prefix : PACKAGE_PREFIXES) {
            if (type.startsWith(prefix + ".")) {
                return type.substring(type.lastIndexOf('.') + 1)
                        .replaceAll("([a-z\\d])([A-Z])", "$1_$2")
                        .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
                        .replaceAll("([A-Za-z])(\\d)", "$1_$2")
                        .replaceAll("(\\d)([A-Za-z])", "$1_$2")
                        .toLowerCase(Locale.ENGLISH);
            }
        }
        return type;
    }
}
