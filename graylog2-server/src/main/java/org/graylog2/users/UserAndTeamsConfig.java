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

@JsonAutoDetect
@AutoValue
@RuntimeTunable(id = "permissions.sharing", feature = "permissions", title = "Permissions Configuration",
        description = "These settings can be used to control which entity sharing options are available.")
public abstract class UserAndTeamsConfig {
    public static final UserAndTeamsConfig DEFAULT_VALUES = create(true, true);

    @JsonProperty("allow_sharing_with_everyone")
    @TunableField(label = "Share with everyone", help = "Control whether it is possible to share with everyone.")
    public abstract boolean sharingWithEveryone();

    @JsonProperty("allow_sharing_with_users")
    @TunableField(label = "Share with users", help = "Control whether it is possible to share with single users.")
    public abstract boolean sharingWithUsers();

    public static UserAndTeamsConfig defaults() {
        return DEFAULT_VALUES;
    }

    @JsonCreator
    public static UserAndTeamsConfig create(
            @JsonProperty("allow_sharing_with_everyone") boolean sharingWithEveryone,
            @JsonProperty("allow_sharing_with_users") boolean sharingWithUsers) {
        return new AutoValue_UserAndTeamsConfig(sharingWithEveryone, sharingWithUsers);
    }
}
