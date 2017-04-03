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

import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.system.NodeId;

import javax.annotation.Nonnull;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

@AutoValue
@WithBeanGetter
public abstract class AuditActor {
    private static final String URN_GRAYLOG_NODE = "urn:graylog:node:";
    private static final String URN_GRAYLOG_USER = "urn:graylog:user:";

    public abstract String urn();

    public static AuditActor user(@Nonnull String username) {
        if (isNullOrEmpty(username)) {
            throw new IllegalArgumentException("username must not be null or empty");
        }
        return new AutoValue_AuditActor(URN_GRAYLOG_USER + username);
    }

    public static AuditActor system(@Nonnull NodeId nodeId) {
        return new AutoValue_AuditActor(URN_GRAYLOG_NODE + requireNonNull(nodeId, "nodeId must not be null").toString());
    }
}
