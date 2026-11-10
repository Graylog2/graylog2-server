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
package org.graylog2.indexer.fieldtypes.mapping;

import jakarta.ws.rs.BadRequestException;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;

class CustomMappingValidationTest {

    private CustomMappingValidation toTest;

    @BeforeEach
    void setUp() {
        toTest = new CustomMappingValidation();
    }

    @Test
    void checkTypePassesForKnownType() {
        assertThatCode(() -> toTest.checkType(new CustomFieldMapping("my_field", "string")))
                .doesNotThrowAnyException();
    }

    @Test
    void checkTypeThrowsForUnknownType() {
        assertThatThrownBy(() -> toTest.checkType(new CustomFieldMapping("my_field", "bogus")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("bogus");
    }

    @Test
    void checkFieldTypeCanBeChangedPassesForRegularField() {
        assertThatCode(() -> toTest.checkFieldTypeCanBeChanged("my_field"))
                .doesNotThrowAnyException();
    }

    @Test
    void checkFieldTypeCanBeChangedThrowsForReservedField() {
        assertThatThrownBy(() -> toTest.checkFieldTypeCanBeChanged(FIELD_TIMESTAMP))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(FIELD_TIMESTAMP);
    }

    @Test
    void checkProfilePassesWhenAllMappingsAreValid() {
        final var profile = profileWith(new CustomFieldMapping("my_field", "string"));

        assertThatCode(() -> toTest.checkProfile(profile))
                .doesNotThrowAnyException();
    }

    @Test
    void checkProfileThrowsWhenAnyMappingHasUnknownType() {
        final var profile = profileWith(
                new CustomFieldMapping("my_field", "string"),
                new CustomFieldMapping("other_field", "bogus"));

        assertThatThrownBy(() -> toTest.checkProfile(profile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("bogus");
    }

    @Test
    void checkProfileThrowsWhenAnyMappingTargetsReservedField() {
        final var profile = profileWith(new CustomFieldMapping(FIELD_TIMESTAMP, "string"));

        assertThatThrownBy(() -> toTest.checkProfile(profile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(FIELD_TIMESTAMP);
    }

    private static IndexFieldTypeProfile profileWith(CustomFieldMapping... mappings) {
        return new IndexFieldTypeProfile(null, "profile", "description",
                new CustomFieldMappings(List.of(mappings)));
    }
}
