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
package org.graylog.events.processor.aggregation;

import org.graylog.events.processor.EventDefinition;
import org.graylog.plugins.views.search.SearchType;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;

/**
 * Implementations of this interface are able to contribute {@link org.graylog.plugins.views.search.SearchType search types}
 * to non-scroll-based event processors.
 * <p>
 * The search types will be added to the original query and their results will be available to {@link org.graylog.events.processor.modifier.EventModifier event modifiers} by the engine.
 */
public interface EventQuerySearchTypeSupplier {
    @Nonnull
    Set<SearchType> additionalSearchTypes(EventDefinition eventDefinition);

    @Nonnull
    Map<String, Object> eventModifierData(Map<String, SearchType.Result> results);
}
