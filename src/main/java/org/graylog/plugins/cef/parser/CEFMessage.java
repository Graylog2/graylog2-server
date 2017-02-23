package org.graylog.plugins.cef.parser;

import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
public abstract class CEFMessage {

    @Nullable
    public abstract DateTime timestamp();

    public abstract int version();
    public abstract String deviceVendor();
    public abstract String deviceProduct();
    public abstract String deviceVersion();
    public abstract String deviceEventClassId();
    public abstract String name();
    public abstract int severity();

    @Nullable
    public abstract String message();

    public abstract Map<String, Object> fields();

    public static Builder builder() {
        return new AutoValue_CEFMessage.Builder();
    }

    public String humanReadableSeverity() {
        switch(severity()) {
            case 0:
            case 1:
            case 2:
            case 3:
                return "LOW";
            case 4:
            case 5:
            case 6:
                return "MEDIUM";
            case 7:
            case 8:
                return "HIGH";
            case 9:
            case 10:
                return "VERY HIGH";
            default:
                return "UNKNOWN";
        }
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
        public abstract Builder severity(int severity);

        public abstract Builder fields(Map<String, Object> fields);

        public abstract Builder message(@Nullable String message);

        public abstract CEFMessage build();
    }

}
