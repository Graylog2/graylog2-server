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

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class UserAndTeamsConfig {
    @JsonProperty
    public abstract boolean sharingWithEveryone();

    @JsonProperty
    public abstract boolean sharingWithUsers();

    @JsonCreator
    public static UserAndTeamsConfig create(
            @JsonProperty("allow_sharing_with_everyone") boolean sharingWithEveryone,
            @JsonProperty("allow_sharing_with_users") boolean sharingWithUsers) {
        return new AutoValue_UserAndTeamsConfig(sharingWithEveryone, sharingWithUsers);
    }
}
