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

import com.github.joschi.jadconfig.ParameterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SortedPathSetConverterTest {
    private SortedPathSetConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new SortedPathSetConverter();
    }

    @Test
    public void testConvertFrom() {
        // Verify path set sizes.
        assertEquals(0, converter.convertFrom("").size());
        assertEquals(0, converter.convertFrom(",").size());
        assertEquals(0, converter.convertFrom(",,,").size());
        assertEquals(1, converter.convertFrom("/another-dir").size());
        assertEquals(1, converter.convertFrom("/another-dir;/some-dir;/finally-dir").size());
        assertEquals(3, converter.convertFrom("/another-dir, /some-dir, /finally-dir").size());

        // Verify path sorting.
        final String unsortedPaths = "/some-dir,/Z-dir,/z-dir,/another-dir/sub,/another-dir";
        final SortedSet<Path> result = converter.convertFrom(unsortedPaths);
        assertEquals(5, result.size());
        assertEquals("/Z-dir,/another-dir,/another-dir/sub,/some-dir,/z-dir",
                     result.stream().map(Path::toString).collect(Collectors.joining(",")),
                     "Paths were not sorted as expected");
    }

    @Test
    public void testConvertTo() {
        TreeSet<Path> sortedSet = new TreeSet<>(Comparator.comparing(Path::toString));
        sortedSet.add(Paths.get("/some-dir"));
        sortedSet.add(Paths.get("/Z-dir"));
        sortedSet.add(Paths.get("/z-dir"));
        sortedSet.add(Paths.get("/another-dir/sub"));
        sortedSet.add(Paths.get("/another-dir"));
        assertEquals("/Z-dir,/another-dir,/another-dir/sub,/some-dir,/z-dir", converter.convertTo(sortedSet));
    }

    @Test
    public void testConvertFromEmpty() {
        assertEquals(new TreeSet<>(), converter.convertFrom(""));
    }

    @Test
    public void testConvertToEmpty() {
        assertEquals("", converter.convertTo(new TreeSet<>()));
    }

    @Test
    public void testConvertFromNull() {
        assertThrows(ParameterException.class, () ->
            converter.convertFrom(null));
    }

    @Test
    public void testConvertToNull() {
        assertThrows(ParameterException.class, () ->
            converter.convertTo(null));
    }
}
