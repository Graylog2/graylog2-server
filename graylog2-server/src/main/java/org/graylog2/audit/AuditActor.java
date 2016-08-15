/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
