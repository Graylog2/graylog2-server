package org.graylog2.audit;

import org.elasticsearch.common.Strings;

import javax.annotation.Nonnull;
import java.util.Objects;

public class AuditActor {

    private static final String URN_GRAYLOG_SERVER = "urn:graylog:server:";

    public static String user(@Nonnull String username) {
        if (Strings.isNullOrEmpty(username)) {
            throw new IllegalArgumentException("username must not be empty");
        }
        if (isURNActor(username)) {
            return username;
        }
        return URN_GRAYLOG_SERVER + "user:" + username;
    }

    public static String system() {
        return URN_GRAYLOG_SERVER + "system";
    }

    public static String unknown() {
        return URN_GRAYLOG_SERVER + "unknown";
    }

    public static boolean isURNActor(String actor) {
        return Objects.requireNonNull(actor, "actor cannot be null").contains(URN_GRAYLOG_SERVER);
    }
}
