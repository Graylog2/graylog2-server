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
package org.graylog.plugins.views.search.rest.scriptingapi.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Splitter;

import javax.annotation.Nullable;
import java.util.List;

public record RequestedField(String name, @Nullable String decorator) {

    public static RequestedField parse(String value) {
        final List<String> parts = Splitter.on(".")
                .limit(2)
                .trimResults()
                .omitEmptyStrings()
                .splitToList(value);

        if (parts.size() == 1) {
            return new RequestedField(parts.get(0), null);
        } else {
            return new RequestedField(parts.get(0), parts.get(1));
        }
    }

    @Override
    public String toString() {
        if (decorator == null) {
            return name;
        } else {
            return name + "." + decorator;
        }
    }

    public boolean hasDecorator(String decoratorName) {
        return decorator != null && decorator.equals(decoratorName);
    }

    public boolean hasDecorator() {
        return decorator != null;
    }
}
