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
import org.graylog2.configuration.tunables.RuntimeTunable;
import org.graylog2.configuration.tunables.TunableField;
import org.graylog2.configuration.tunables.When;
import org.graylog2.plugin.Version;
import org.threeten.extra.PeriodDuration;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@JsonAutoDetect
@AutoValue
@RuntimeTunable(id = "users.configuration", feature = "users", title = "Users Configuration",
        description = "These settings can be used to set a global session timeout value and control access tokens.")
public abstract class UserConfiguration {
    private static final boolean IS_BEFORE_VERSION_6_2 = !Version.CURRENT_CLASSPATH.sameOrHigher(Version.from(6, 2, 0));

    //Starting with graylog version 6.2, external users are not allowed to own access tokens by default.
    // Before this version, it is allowed, to not introduce a breaking change:
    // Similarly, starting from version 6.2, creation of tokens is restricted to admins only:
    public static final UserConfiguration DEFAULT_VALUES = create(false, Duration.of(8, ChronoUnit.HOURS), IS_BEFORE_VERSION_6_2, !IS_BEFORE_VERSION_6_2, PeriodDuration.of(Duration.ofDays(30)));
    //In case the installation is upgraded, we apply some less strict defaults:
    public static final UserConfiguration DEFAULT_VALUES_FOR_UPGRADE = create(false, Duration.of(8, ChronoUnit.HOURS), IS_BEFORE_VERSION_6_2, false, PeriodDuration.of(Duration.ofDays(30)));

    @JsonProperty("enable_global_session_timeout")
    @TunableField(label = "Enable global session timeout", help = "If enabled, it will be set for all the users.")
    public abstract boolean enableGlobalSessionTimeout();

    @JsonProperty("global_session_timeout_interval")
    @TunableField(label = "Global session timeout interval",
            help = "Sessions automatically end after this amount of time, unless they are actively used.",
            enabledWhen = @When(property = "enable_global_session_timeout", in = "true"))
    public abstract Duration globalSessionTimeoutInterval();

    @JsonProperty("allow_access_token_for_external_user")
    @TunableField(label = "Allow access token for external users", help = "If enabled, it will allow external users to create access tokens.")
    public abstract boolean allowAccessTokenForExternalUsers();

    @JsonProperty("restrict_access_token_to_admins")
    @TunableField(label = "Restrict access token creation to admins", help = "If enabled, only admins can create access tokens.")
    public abstract boolean restrictAccessTokenToAdmins();

    @JsonProperty("default_ttl_for_new_tokens")
    @TunableField(label = "Default TTL for new tokens", help = "Tokens will be automatically invalidated after this amount of time.",
            minUnit = ChronoUnit.HOURS)
    public abstract PeriodDuration defaultTTLForNewTokens();

    public static UserConfiguration defaults() {
        return DEFAULT_VALUES;
    }

    @JsonCreator
    public static UserConfiguration create(
            @JsonProperty("enable_global_session_timeout") boolean enableGlobalSessionTimeout,
            @JsonProperty("global_session_timeout_interval") Duration globalSessionTimeoutInterval,
            @JsonProperty("allow_access_token_for_external_user") boolean allowAccessTokenForExternalUsers,
            @JsonProperty("restrict_access_token_to_admins") boolean restrictAccessTokensToAdmins,
            @JsonProperty("default_ttl_for_new_tokens") PeriodDuration defaultTTLForNewTokens) {
        return new AutoValue_UserConfiguration(enableGlobalSessionTimeout, globalSessionTimeoutInterval,
                allowAccessTokenForExternalUsers, restrictAccessTokensToAdmins, defaultTTLForNewTokens);
    }
}
