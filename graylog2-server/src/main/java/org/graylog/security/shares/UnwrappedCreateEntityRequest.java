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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.annotation.Nullable;

import java.util.Optional;

public class UnwrappedCreateEntityRequest<T> {
    @JsonUnwrapped
    private T entity;

    @Nullable
    @JsonProperty("share_request")
    public EntityShareRequest shareRequest;

    public UnwrappedCreateEntityRequest() {
    }

    public UnwrappedCreateEntityRequest(T entity, @Nullable EntityShareRequest shareRequest) {
        this.shareRequest = shareRequest;
        this.entity = entity;
    }

    public T getEntity() {
        return entity;
    }

    public void setEntity(T entity) {
        this.entity = entity;
    }

    public Optional<EntityShareRequest> getShareRequest() {
        return Optional.ofNullable(shareRequest);
    }

    public void setShareRequest(@Nullable EntityShareRequest shareRequest) {
        this.shareRequest = shareRequest;
    }
}
