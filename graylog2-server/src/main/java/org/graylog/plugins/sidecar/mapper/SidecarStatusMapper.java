package org.graylog.plugins.sidecar.mapper;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class SidecarStatusMapper {
    private static final Pattern searchQueryStatusRegex = Pattern.compile("\\bstatus:(running|failing|unknown)\\b", CASE_INSENSITIVE);
    public enum Status {
        RUNNING(0), UNKNOWN(1), FAILING(2);

        private final int statusCode;

        Status(int statusCode) {
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public static Status fromStatusCode(int statusCode) {
            switch (statusCode) {
                case 0: return RUNNING;
                case 2: return FAILING;
                default: return UNKNOWN;
            }
        }

        public static Status fromString(String statusString) {
            return valueOf(statusString.toUpperCase(Locale.ENGLISH));
        }
    }

    /**
     * Replaces status strings in search query with their number representations,
     * e.g. <code>status:running</code> will be transformed into <code>status:0</code>.
     *
     * @param query Search query that may contain one or more status strings
     * @return Search query with all status strings replaced with status codes
     */
    public String replaceStringStatusSearchQuery(String query) {
        final Matcher matcher = searchQueryStatusRegex.matcher(query);
        final StringBuffer stringBuffer = new StringBuffer();
        while(matcher.find()) {
            final String status = matcher.group(1);
            matcher.appendReplacement(stringBuffer, "status:" + Status.fromString(status).getStatusCode());
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }
}
