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

import com.google.common.collect.ForwardingList;

import java.util.ArrayList;
import java.util.List;

public class ReferenceList extends ForwardingList<Reference> implements Reference {
    private final List<Reference> list;

    public ReferenceList(List<Reference> list) {
        this.list = list;
    }

    public ReferenceList() {
        this(new ArrayList<>());
    }

    @Override
    protected List<Reference> delegate() {
        return list;
    }
}
