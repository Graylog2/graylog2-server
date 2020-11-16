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
package org.graylog2.gettingstarted;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Set;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
public abstract class GettingStartedState {
    /**
     * A map of Version.getMajor + Version.getMinor -> true/false.
     * <br/>
     * A true value means the automatic display of the getting started page has been turned of for that minor version (we don't show it again for patch versions).
     * Missing values are treated as false.
     *
     * @return dismissal state of getting started pages across all stored versions
     */
    @JsonProperty
    public abstract Set<String> dismissedInVersions();

    @JsonCreator
    public static GettingStartedState create(@JsonProperty("dismissed_in_versions") Set<String> dismissedInVersions) {
        return new AutoValue_GettingStartedState(dismissedInVersions);
    }

}
