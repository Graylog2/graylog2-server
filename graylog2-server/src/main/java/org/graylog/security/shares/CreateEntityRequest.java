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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;

import java.util.Objects;
import java.util.Optional;

@JsonAutoDetect
public class CreateEntityRequest<T> {
    @Valid
    private final T entity;
    @Nullable
    private final EntityShareRequest shareRequest;

    private CreateEntityRequest(T entity, @Nullable EntityShareRequest shareRequest) {
        this.entity = entity;
        this.shareRequest = shareRequest;
    }

    @Valid
    @JsonProperty("entity")
    public T entity() {
        return entity;
    }

    @JsonProperty("share_request")
    public Optional<EntityShareRequest> shareRequest() {
        return Optional.ofNullable(shareRequest);
    }

    @JsonCreator
    public static <T> CreateEntityRequest<T> create(@JsonProperty("entity") T entity,
                                                    @JsonProperty("share_request") @Nullable EntityShareRequest shareRequest) {
        return new CreateEntityRequest<>(entity, shareRequest);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final CreateEntityRequest<?> that = (CreateEntityRequest<?>) o;
        return Objects.equals(entity, that.entity) && Objects.equals(shareRequest, that.shareRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, shareRequest);
    }

    @Override
    public String toString() {
        return "CreateEntityRequest{entity=" + entity + ", shareRequest=" + shareRequest + '}';
    }
}
