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
package org.graylog.security.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@WithBeanGetter
public abstract class CreateEntityRequest<T> {
    @JsonProperty("entity")
    public abstract T entity();

    @JsonProperty("share_request")
    public abstract Optional<EntityShareRequest> shareRequest();

    @JsonCreator
    public static <T> CreateEntityRequest<T> create(@JsonProperty("entity") T entity,
                                                    @JsonProperty("share_request") @Nullable EntityShareRequest shareRequest) {
        return new AutoValue_CreateEntityRequest(entity, Optional.ofNullable(shareRequest));
    }
}
