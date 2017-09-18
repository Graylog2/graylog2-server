package org.graylog.plugins.cef.parser;

import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
public abstract class CEFMessage {
    @AutoValue
    public static abstract class Severity {
        @VisibleForTesting
        static final Severity UNKNOWN = create(-1, "Unknown");

        private static final Map<String, Range<Integer>> VALUES = ImmutableMap.<String, Range<Integer>>builder()
                .put("Low", Range.closed(0, 3))
                .put("Medium", Range.closed(4, 6))
                .put("High", Range.closed(7, 8))
                .put("Very-High", Range.closed(9, 10))
                .put("Unknown", Range.singleton(-1))
                .build();

        public abstract int numeric();
        public abstract String text();

        @VisibleForTesting
        static Severity create(int numeric, String text) {
            return new AutoValue_CEFMessage_Severity(numeric, text);
        }

        public static Severity parse(String text) {
            final String s = text.trim();
            try {
                final int i = Integer.parseInt(s);
                return parseNumericSeverity(i);
            } catch (NumberFormatException e) {
                return parseTextualSeverity(s);
            }
        }

        private static Severity parseNumericSeverity(int n) {
            for (Map.Entry<String, Range<Integer>> entry : Severity.VALUES.entrySet()) {
                if (entry.getValue().contains(n)) {
                    return create(n, entry.getKey());
                }
            }
            return UNKNOWN;
        }

        private static Severity parseTextualSeverity(String s) {
            for (Map.Entry<String, Range<Integer>> entry : Severity.VALUES.entrySet()) {
                final String key = entry.getKey();
                if (key.equalsIgnoreCase(s)) {
                    return create(entry.getValue().lowerEndpoint(), key);
                }
            }
            return UNKNOWN;
        }
    }

    @Nullable
    public abstract DateTime timestamp();

    public abstract int version();
    public abstract String deviceVendor();
    public abstract String deviceProduct();
    public abstract String deviceVersion();
    public abstract String deviceEventClassId();
    public abstract String name();
    @Nullable
    public abstract String hostname();
    public abstract Severity severity();

    @Nullable
    public abstract String message();

    public abstract Map<String, Object> fields();

    public static Builder builder() {
        return new AutoValue_CEFMessage.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder timestamp(DateTime timestamp);
        public abstract Builder version(int version);
        public abstract Builder deviceVendor(String vendor);
        public abstract Builder deviceProduct(String product);
        public abstract Builder deviceVersion(String version);
        public abstract Builder deviceEventClassId(String eventClassId);
        public abstract Builder name(String name);
        public abstract Builder hostname(String name);
        public abstract Builder severity(Severity severity);

        public abstract Builder fields(Map<String, Object> fields);

        public abstract Builder message(@Nullable String message);

        public abstract CEFMessage build();
    }

}
