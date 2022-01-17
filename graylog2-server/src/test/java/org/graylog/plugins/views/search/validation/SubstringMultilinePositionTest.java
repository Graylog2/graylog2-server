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
package org.graylog.plugins.views.search.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubstringMultilinePositionTest {

    @Test
    void computePosition() {
        final List<SubstringMultilinePosition> positions = SubstringMultilinePosition.compute("foo:bar AND \nlorem:$param$", "$param$");
        assertThat(positions.size()).isEqualTo(1);
        final SubstringMultilinePosition position = positions.get(0);
        assertThat(position.getLine()).isEqualTo(2);
        assertThat(position.getBeginColumn()).isEqualTo(6);
        assertThat(position.getEndColumn()).isEqualTo(13);
    }

    @Test
    void computePositionOfMoreOccurences() {
        final String input = "foo:$param$ OR lorem:$param$ OR\nbar:$param$";
        final List<SubstringMultilinePosition> positions = SubstringMultilinePosition.compute(input, "$param$");
        assertThat(positions.size()).isEqualTo(3);
    }

    @Test
    void computePositionOfNonexistentMatch() {
        assertThat(SubstringMultilinePosition.compute("foo:bar", "baz")).isEmpty();
    }
}
