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
package org.graylog2.configuration.validators;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import org.graylog2.plugin.Version;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.storage.SearchVersion.Distribution.ELASTICSEARCH;
import static org.graylog2.storage.SearchVersion.Distribution.OPENSEARCH;
import static org.junit.jupiter.api.Assertions.*;

class DetectedSearchVersionValidatorTest {

    private final Validator<SearchVersion> validator = new ElasticsearchVersionValidator();

    @Test
    void validateMajorVersion() {
        assertDoesNotThrow(() -> validator.validate("OS1", SearchVersion.create(OPENSEARCH, com.github.zafarkhaja.semver.Version.forIntegers(1, 0, 0))));
        assertDoesNotThrow(() -> validator.validate("ES7", SearchVersion.create(ELASTICSEARCH, com.github.zafarkhaja.semver.Version.forIntegers(7, 0, 0))));
    }

    @Test
    void testPatchVersion() {
        assertDoesNotThrow(() -> validator.validate("ES7", SearchVersion.create(ELASTICSEARCH, com.github.zafarkhaja.semver.Version.forIntegers(7, 10, 2))));
    }

    @Test
    void testInvalidCombination() {
        assertThrows(ValidationException.class, () -> validator.validate("ES5", SearchVersion.create(ELASTICSEARCH, com.github.zafarkhaja.semver.Version.forIntegers(5, 0, 0))));
    }
}
