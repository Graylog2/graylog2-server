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
package org.graylog2.shared.utilities;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilsTest {
    @Test
    public void testHumanReadable() {
        assertThat(StringUtils.humanReadableByteCount(1024L * 1024L * 1024L * 5L + 1024L * 1024L * 512L)).isEqualTo("5.5 GiB");
        assertThat(StringUtils.humanReadableByteCount(1024L * 1024L * 1024L * 5L)).isEqualTo("5.0 GiB");
        assertThat(StringUtils.humanReadableByteCount(1024L * 1024L * 4L + 1024L * 900L)).isEqualTo("4.9 MiB");
        assertThat(StringUtils.humanReadableByteCount(1023)).isEqualTo("1023 B");
        assertThat(StringUtils.humanReadableByteCount(1024)).isEqualTo("1.0 KiB");
        assertThat(StringUtils.humanReadableByteCount(1024L * 1024L * 1024L * 1024L * 5L + 1024L * 1024L * 512L)).isEqualTo("5.0 TiB");
        assertThat(StringUtils.humanReadableByteCount(1024L * 5L + 512L)).isEqualTo("5.5 KiB");
    }

    @Test
    public void testSplitByComma() {
        Assertions.assertThat(StringUtils.splitByComma((Set<String>) null))
                .isInstanceOf(Set.class)
                .isEmpty();

        Assertions.assertThat(StringUtils.splitByComma(Collections.emptySet()))
                .isInstanceOf(Set.class)
                .isEmpty();

        Assertions.assertThat(StringUtils.splitByComma(ImmutableList.of("one", "two,three,", "", " ")))
                .hasSize(3)
                .containsExactlyInAnyOrder("one", "two", "three");


        Assertions.assertThat(StringUtils.splitByComma(ImmutableList.of("one", "two,three")))
                .hasSize(3)
                .containsExactlyInAnyOrder("one", "two", "three");


        Assertions.assertThat(StringUtils.splitByComma(ImmutableSet.of("one", "two,three")))
                .hasSize(3)
                .containsExactlyInAnyOrder("one", "two", "three");


    }
}
