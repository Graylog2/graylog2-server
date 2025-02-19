/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.users;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.Version;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class UserConfiguration {
    private static final boolean IS_BEFORE_VERSION_6_2 = !Version.CURRENT_CLASSPATH.sameOrHigher(Version.from(6, 2, 0));

    //Starting with graylog version 6.2, external users are not allowed to own access tokens by default.
    // Before this version, it is allowed, to not introduce a breaking change:
    // Similarly, starting from version 6.2, creation of tokens is restricted to admins only:
    public static final UserConfiguration DEFAULT_VALUES = create(false, Duration.of(8, ChronoUnit.HOURS), IS_BEFORE_VERSION_6_2, !IS_BEFORE_VERSION_6_2);
    //In case the installation is upgraded, we apply some less strict defaults:
    public static final UserConfiguration DEFAULT_VALUES_FOR_UPGRADE = create(false, Duration.of(8, ChronoUnit.HOURS), IS_BEFORE_VERSION_6_2, false);

    @JsonProperty("enable_global_session_timeout")
    public abstract boolean enableGlobalSessionTimeout();

    @JsonProperty("global_session_timeout_interval")
    public abstract Duration globalSessionTimeoutInterval();

    @JsonProperty("allow_access_token_for_external_user")
    public abstract boolean allowAccessTokenForExternalUsers();

    @JsonProperty("restrict_access_token_to_admins")
    public abstract boolean restrictAccessTokenToAdmins();

    @JsonCreator
    public static UserConfiguration create(
            @JsonProperty("enable_global_session_timeout") boolean enableGlobalSessionTimeout,
            @JsonProperty("global_session_timeout_interval") Duration globalSessionTimeoutInterval,
            @JsonProperty("allow_access_token_for_external_user") boolean allowAccessTokenForExternalUsers,
            @JsonProperty("restrict_access_token_to_admins") boolean restrictAccessTokensToAdmins) {
        return new AutoValue_UserConfiguration(enableGlobalSessionTimeout, globalSessionTimeoutInterval, allowAccessTokenForExternalUsers, restrictAccessTokensToAdmins);
    }
}
