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
package org.graylog.plugins.pipelineprocessor.ast.expressions;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineOpsTest {
    private static final EvaluationContext CTX = EvaluationContext.emptyContext();

    @Test
    void compareLongWithDoubleThrowsClassCastException() {
        assertThatThrownBy(() -> PipelineOps.compare(">", 1L, 2.0d)).isInstanceOf(ClassCastException.class);
    }

    @Test
    void compareNullThrowsNpeNamingTheParameter() {
        assertThatThrownBy(() -> PipelineOps.compare(">", null, 2L))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("\"leftValue\" is null");
    }

    @Test
    void compareDateTimes() {
        final DateTime a = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
        assertThat(PipelineOps.compare("<", a, a.plusSeconds(1))).isTrue();
        assertThat(PipelineOps.compare(">=", a, a)).isTrue();
    }

    @Test
    void toBoolTreatsNullAsFalse() {
        assertThat(PipelineOps.toBool(null)).isFalse();
        assertThat(PipelineOps.toBool(Boolean.TRUE)).isTrue();
    }

    @Test
    void arrayLiteralIsArrayListAndKeepsNulls() {
        assertThat(PipelineOps.arrayLiteral(new Object[]{1L, null}))
                .isInstanceOf(ArrayList.class).containsExactly(1L, null);
    }

    @Test
    void mapLiteralDropsNullValuesAndIsHashMap() {
        assertThat(PipelineOps.mapLiteral(new String[]{"a", "b"}, new Object[]{1L, null}))
                .isInstanceOf(HashMap.class).containsOnlyKeys("a");
    }

    @Test
    void indexedAccessRejectsNonIndexable() {
        assertThatThrownBy(() -> PipelineOps.indexedAccess(CTX, 0L, "nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not an Array, List or Iterable");
    }

    @Test
    void messageFieldNullNameIsNull() {
        assertThat(PipelineOps.messageField(CTX, null)).isNull();
    }
}
