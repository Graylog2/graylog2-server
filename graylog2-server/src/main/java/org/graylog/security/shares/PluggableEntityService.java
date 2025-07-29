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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.grn.GRN;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Service to simplify the interaction with multiple pluggable entity handlers.
 */
@Singleton
public class PluggableEntityService {
    Set<PluggableEntityHandler> handlers;

    @Inject
    public PluggableEntityService(Set<PluggableEntityHandler> handlers) {
        this.handlers = handlers;
    }

    public void onCreate(GRN entity, Set<GRN> collections) {
        handlers.forEach(handler -> handler.onCreate(entity, collections));
    }

    public Predicate<GRN> excludeTypesFilter() {
        return grn -> handlers.stream()
                .noneMatch(handler -> handler.entityFilter().test(grn));
    }

    public Stream<GRN> expand(GRN grn) {
        return handlers.stream()
                .flatMap(handler -> handler.expand(grn));
    }
}
