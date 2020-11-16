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
package org.graylog2.contentpacks.model.entities.references;

import com.google.common.collect.ForwardingMap;

import java.util.HashMap;
import java.util.Map;

public class ReferenceMap extends ForwardingMap<String, Reference> implements Reference {
    private final Map<String, Reference> map;

    public ReferenceMap(Map<String, Reference> map) {
        this.map = map;
    }

    public ReferenceMap() {
        this(new HashMap<>());
    }

    @Override
    protected Map<String, Reference> delegate() {
        return map;
    }
}
