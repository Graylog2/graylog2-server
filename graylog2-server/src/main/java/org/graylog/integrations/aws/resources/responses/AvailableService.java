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
package org.graylog.integrations.aws.resources.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class AvailableService {

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String POLICY = "policy";
    private static final String HELPER_TEXT = "helper_text";
    private static final String LEARN_MORE_LINK = "learn_more_link";

    @JsonProperty(NAME)
    public abstract String name();

    @JsonProperty(DESCRIPTION)
    public abstract String description();

    @JsonProperty(POLICY)
    public abstract String policy();

    @JsonProperty(HELPER_TEXT)
    public abstract String helperText();

    @JsonProperty(LEARN_MORE_LINK)
    public abstract String learnMoreLink();

    public static AvailableService create(@JsonProperty(NAME) String name,
                                          @JsonProperty(DESCRIPTION) String description,
                                          @JsonProperty(POLICY) String policy,
                                          @JsonProperty(HELPER_TEXT) String helperText,
                                          @JsonProperty(LEARN_MORE_LINK) String learnMoreLink) {
        return new AutoValue_AvailableService(name, description, policy, helperText, learnMoreLink);
    }
}