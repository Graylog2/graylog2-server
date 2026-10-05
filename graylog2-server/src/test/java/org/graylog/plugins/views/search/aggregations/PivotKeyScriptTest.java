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
package org.graylog.plugins.views.search.aggregations;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PivotKeyScriptTest {
    private static final String KEY_SEPARATOR_PHRASE = " + \"\u2E31\" + ";

    @Test
    void passesFieldNamesAsParametersInsteadOfInterpolatingThemIntoTheSource() {
        final PivotKeyScript script = PivotKeyScript.create(List.of("source", "http.response.status_code"),
                KEY_SEPARATOR_PHRASE);

        assertThat(script.source())
                .doesNotContain("source")
                .doesNotContain("http.response.status_code")
                .contains("doc[params.field0]")
                .contains("doc[params.field1]")
                .contains("params.missingValue");

        assertThat(script.params()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "field0", "source",
                "field1", "http.response.status_code",
                "missingValue", MissingBucketConstants.MISSING_BUCKET_NAME));
    }

    @Test
    void joinsFieldExpressionsWithTheKeySeparatorPhrase() {
        assertThat(PivotKeyScript.create(List.of("a"), KEY_SEPARATOR_PHRASE).source())
                .doesNotContain(KEY_SEPARATOR_PHRASE);
        assertThat(PivotKeyScript.create(List.of("a", "b"), KEY_SEPARATOR_PHRASE).source())
                .containsOnlyOnce(KEY_SEPARATOR_PHRASE);
        assertThat(PivotKeyScript.create(List.of("a", "b", "c"), KEY_SEPARATOR_PHRASE).source())
                .contains(KEY_SEPARATOR_PHRASE);
    }

    @Test
    void generatesIdenticalSourceForDifferentFieldsOfTheSameCardinality() {
        assertThat(PivotKeyScript.create(List.of("source", "message"), KEY_SEPARATOR_PHRASE).source())
                .as("the script source must be reusable so that the backend does not compile one script per field combination")
                .isEqualTo(PivotKeyScript.create(List.of("timestamp", "gl2_source_input"), KEY_SEPARATOR_PHRASE).source());
    }

    @Test
    void keepsDuplicateFieldsAsSeparateParameters() {
        final PivotKeyScript script = PivotKeyScript.create(List.of("source", "source"), KEY_SEPARATOR_PHRASE);

        assertThat(script.params()).containsEntry("field0", "source");
        assertThat(script.params()).containsEntry("field1", "source");
    }
}
