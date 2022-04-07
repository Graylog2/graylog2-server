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
package org.graylog2.rest.models.users.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CreateUserRequest {

    @JsonProperty
    public abstract String username();

    @JsonProperty
    public abstract String password();

    @JsonProperty
    public abstract String email();

    @JsonProperty
    public abstract String fullName();

    @JsonProperty
    public abstract List<String> permissions();

    @JsonProperty
    @Nullable
    public abstract String timezone();

    @JsonProperty
    @Nullable
    public abstract Long sessionTimeoutMs();

    @JsonProperty
    @Nullable
    public abstract Startpage startpage();

    @JsonProperty
    @Nullable
    public abstract List<String> roles();

    @JsonProperty
    public abstract boolean isServiceAccount();

    @JsonCreator
    public static CreateUserRequest create(@JsonProperty("username") @NotEmpty String username,
                                           @JsonProperty("password") @NotEmpty String password,
                                           @JsonProperty("email") @Email String email,
                                           @JsonProperty("full_name") @NotEmpty String fullName,
                                           @JsonProperty("permissions") @NotNull List<String> permissions,
                                           @JsonProperty("timezone") @Nullable String timezone,
                                           @JsonProperty("session_timeout_ms") @Nullable @Min(1) Long sessionTimeoutMs,
                                           @JsonProperty("startpage") @Nullable Startpage startpage,
                                           @JsonProperty("roles") @Nullable List<String> roles,
                                           @JsonProperty("service_account") @Nullable Boolean isServiceAccount) {
        return new AutoValue_CreateUserRequest(username, password, email, fullName,
                                               permissions, timezone, sessionTimeoutMs, startpage, roles,
                isServiceAccount != null && isServiceAccount);
    }
}
