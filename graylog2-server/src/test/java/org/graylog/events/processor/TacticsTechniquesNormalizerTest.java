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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TacticsTechniquesNormalizerTest {
    @Test
    void normalizeUppercasesAndTrims() {
        assertThat(TacticsTechniquesNormalizer.normalize(List.of("ta0002", " T1059 ", "t1059.001")))
                .containsExactly("TA0002", "T1059", "T1059.001");
    }

    @Test
    void normalizeDropsNullAndEmptyEntries() {
        assertThat(TacticsTechniquesNormalizer.normalize(Arrays.asList("TA0002", null, "", "  ", "T1059")))
                .containsExactly("TA0002", "T1059");
    }

    @Test
    void normalizeNullInputReturnsEmpty() {
        assertThat(TacticsTechniquesNormalizer.normalize(null)).isEmpty();
    }

    @Test
    void normalizeDeduplicatesEntries() {
        assertThat(TacticsTechniquesNormalizer.normalize(List.of("TA0002", "ta0002", "T1059", "T1059", "t1059")))
                .containsExactly("TA0002", "T1059");
    }

    @Test
    void isValidAcceptsCanonicalShapes() {
        assertThat(TacticsTechniquesNormalizer.isValid("TA0002")).isTrue();
        assertThat(TacticsTechniquesNormalizer.isValid("T1059")).isTrue();
        assertThat(TacticsTechniquesNormalizer.isValid("T1059.001")).isTrue();
    }

    @Test
    void isValidRejectsOther() {
        assertThat(TacticsTechniquesNormalizer.isValid("t1059")).isFalse();
        assertThat(TacticsTechniquesNormalizer.isValid("T999")).isFalse();
        assertThat(TacticsTechniquesNormalizer.isValid("attack.execution")).isFalse();
        assertThat(TacticsTechniquesNormalizer.isValid(null)).isFalse();
        assertThat(TacticsTechniquesNormalizer.isValid("")).isFalse();
    }
}
