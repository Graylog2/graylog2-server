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
package org.graylog2.indexer.indices.events;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;

import java.util.Set;

@AutoValue
@WithBeanGetter
public abstract class IndicesDeletedEvent {
    public abstract Set<String> indices();

    public static IndicesDeletedEvent create(Set<String> indices) {
        return new AutoValue_IndicesDeletedEvent(ImmutableSet.copyOf(indices));
    }

    public static IndicesDeletedEvent create(String index) {
        return new AutoValue_IndicesDeletedEvent(ImmutableSet.of(index));
    }
}
