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
package org.graylog2.configuration.converters;

import com.github.joschi.jadconfig.Converter;
import org.graylog2.storage.SearchVersion;

public class MajorVersionConverter implements Converter<SearchVersion> {
    @Override
    public SearchVersion convertFrom(String value) {
        try {
            // only major version - we know it's elasticsearch
            final int majorVersion = Integer.parseInt(value);
            return SearchVersion.elasticsearch(majorVersion, 0, 0);
        } catch (NumberFormatException nfe) {
            // It's probably a distribution:version format
            // caution, this format assumes full version X.Y.Z, not just major number
            return SearchVersion.decode(value);
        }
    }

    @Override
    public String convertTo(SearchVersion value) {
        return value.encode();
    }
}
