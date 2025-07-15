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
package org.graylog.datanode;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PathListConverter implements Converter<List<Path>> {

    public static final String DELIMITER = ",";

    @Override
    public List<Path> convertFrom(String value) {
        if (value == null) {
            return null;
        } else if (value.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Arrays.stream(value.split(DELIMITER)).map(Path::of).collect(Collectors.toList());
        }
    }

    @Override
    public String convertTo(List<Path> value) {
        if (value == null) {
            throw new ParameterException("String list of Paths must not be null.");
        }

        return value.stream().map(Path::toString).collect(Collectors.joining(DELIMITER));
    }
}
