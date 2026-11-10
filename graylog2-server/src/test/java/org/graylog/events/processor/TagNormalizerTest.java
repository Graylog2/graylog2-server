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
package org.graylog.events.processor;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class TagNormalizerTest {

    @Test
    void returnsEmptySetForNullInput() {
        assertThat(TagNormalizer.normalize(null)).isEmpty();
    }

    @Test
    void returnsEmptySetForEmptyInput() {
        assertThat(TagNormalizer.normalize(Collections.emptyList())).isEmpty();
    }

    @Test
    void trimsLowercasesAndDedupes() {
        final ImmutableSet<String> result = TagNormalizer.normalize(
                Arrays.asList("  Phishing  ", "lateral-MOVEMENT", "phishing"));
        assertThat(result).containsExactlyInAnyOrder("phishing", "lateral-movement");
    }

    @Test
    void dropsBlanksAndNulls() {
        final ImmutableSet<String> result = TagNormalizer.normalize(
                Arrays.asList("auth", "", "   ", null, "auth"));
        assertThat(result).containsExactly("auth");
    }
}
