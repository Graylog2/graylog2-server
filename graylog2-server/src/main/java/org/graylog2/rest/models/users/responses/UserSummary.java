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
package org.graylog2.rest.models.users.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.rest.models.users.requests.Startpage;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class UserSummary {

    @JsonProperty
    @Nullable
    public abstract String id();

    @JsonProperty
    public abstract String username();

    @JsonProperty
    public abstract String email();

    @JsonProperty
    public abstract String fullName();

    @JsonProperty
    public abstract List<String> permissions();

    @JsonProperty
    @Nullable
    public abstract Map<String, Object> preferences();

    @JsonProperty
    @Nullable
    public abstract String timezone();

    @JsonProperty
    @Nullable
    public abstract Long sessionTimeoutMs();

    @JsonProperty("read_only")
    public abstract boolean readOnly();

    @JsonProperty
    public abstract boolean external();

    @JsonProperty
    @Nullable
    public abstract Startpage startpage();

    @JsonProperty
    @Nullable
    public abstract Set<String> roles();

    @JsonProperty("session_active")
    public abstract boolean sessionActive();

    @JsonProperty("last_activity")
    @Nullable
    public abstract Date lastActivity();

    @JsonProperty("client_address")
    @Nullable
    public abstract String clientAddress();

    @JsonProperty("service_account")
    public abstract boolean isServiceAccount();

    @JsonCreator
    public static UserSummary create(@JsonProperty("id") @Nullable String id,
                                     @JsonProperty("username") String username,
                                     @JsonProperty("email") String email,
                                     @JsonProperty("full_name") @Nullable String fullName,
                                     @JsonProperty("permissions") @Nullable List<String> permissions,
                                     @JsonProperty("preferences") @Nullable Map<String, Object> preferences,
                                     @JsonProperty("timezone") @Nullable String timezone,
                                     @JsonProperty("session_timeout_ms") @Nullable Long sessionTimeoutMs,
                                     @JsonProperty("read_only") boolean readOnly,
                                     @JsonProperty("external") boolean external,
                                     @JsonProperty("startpage") @Nullable Startpage startpage,
                                     @JsonProperty("roles") @Nullable Set<String> roles,
                                     @JsonProperty("session_active") boolean sessionActive,
                                     @JsonProperty("last_activity") @Nullable Date lastActivity,
                                     @JsonProperty("client_address") @Nullable String clientAddress,
                                     @JsonProperty("service_account") boolean isServiceAccount) {
        return new AutoValue_UserSummary(id,
                                         username,
                                         email,
                                         fullName,
                                         permissions,
                                         preferences,
                                         timezone,
                                         sessionTimeoutMs,
                                         readOnly,
                                         external,
                                         startpage,
                                         roles,
                                         sessionActive,
                                         lastActivity,
                                         clientAddress,
                                         isServiceAccount);
    }
}
