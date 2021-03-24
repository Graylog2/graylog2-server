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
import com.github.joschi.jadconfig.ParameterException;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SortedPathSetConverter implements Converter<SortedSet<Path>> {
    protected static final String SEPARATOR = ",";

    @Override
    public SortedSet<Path> convertFrom(String value) {
        if (value == null) {
            throw new ParameterException("Path list must not be null.");
        }

        return Arrays.stream(value.split(SEPARATOR))
                     .map(StringUtils::trimToNull)
                     .filter(Objects::nonNull)
                     .map(Paths::get)
                     .collect(Collectors.toCollection(sortedPathSupplier()));
    }

    @Override
    public String convertTo(SortedSet<Path> value) {
        if (value == null) {
            throw new ParameterException("String list of Paths must not be null.");
        }

        return value.stream().map(Path::toString).collect(Collectors.joining(","));
    }

    /**
     * @return {@link Supplier<TreeSet>} which sorts based on {@literal path.toString()}.
     *         Sorting is intentionally performed on a case-sensitive basis, since paths
     *         are also case-sensitive.
     */
    private Supplier<TreeSet<Path>> sortedPathSupplier() {
        return () -> new TreeSet<>(Comparator.comparing(Path::toString));
    }
}
