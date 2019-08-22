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
package org.graylog2.email.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.net.URI;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class EmailConfiguration {

    @JsonProperty
    public abstract boolean enabled();

    @JsonProperty
    @Nullable
    public abstract String hostname();

    @JsonProperty
    public abstract int port();

    @JsonProperty
    public abstract boolean useAuth();

    @JsonProperty
    public abstract boolean useTls();

    @JsonProperty
    public abstract boolean useSsl();

    @JsonProperty
    @Nullable
    public abstract String username();

    @JsonProperty
    @Nullable
    public abstract String password();

    @JsonProperty
    @Nullable
    public abstract String fromEmail();

    @JsonProperty
    @Nullable
    public abstract URI webInterfaceUri();

    @JsonCreator
    public static EmailConfiguration create(@JsonProperty("enabled") boolean enabled,
                                            @JsonProperty("hostname") String hostname,
                                            @JsonProperty("port") int port,
                                            @JsonProperty("use_auth") boolean useAuth,
                                            @JsonProperty("use_tls") boolean useTls,
                                            @JsonProperty("use_ssl") boolean useSsl,
                                            @JsonProperty("username") String username,
                                            @JsonProperty("password") String password,
                                            @JsonProperty("from_email") String fromEmail,
                                            @JsonProperty("web_interface_uri") URI webInterfaceUri) {
        return new AutoValue_EmailConfiguration(
                enabled, hostname, port, useAuth, useTls, useSsl, username, password, fromEmail, webInterfaceUri
        );
    }

    public static EmailConfiguration create() {
        return create(false,
                null,
                25,
                false,
                true,
                false,
                null,
                null,
                null,
                null);
    }

}

