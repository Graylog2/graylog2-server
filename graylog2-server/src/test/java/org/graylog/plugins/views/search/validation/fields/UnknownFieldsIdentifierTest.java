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
package org.graylog.plugins.views.search.validation.fields;

import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnknownFieldsIdentifierTest {

    private UnknownFieldsIdentifier toTest;
    private MappedFieldTypesService mappedFieldTypesService;
    private MappedFieldTypeDTO existingFieldTypeDTO;
    private ValidationRequest validationRequest;

    @BeforeEach
    public void setUp() {
        validationRequest = mock(ValidationRequest.class);
        existingFieldTypeDTO = MappedFieldTypeDTO.create("existingField", FieldTypes.Type.builder().type("date").build());
        mappedFieldTypesService = mock(MappedFieldTypesService.class);
        when(mappedFieldTypesService.fieldTypesByStreamIds(any(), any())).thenReturn(Collections.singleton(existingFieldTypeDTO));

        toTest = new UnknownFieldsIdentifier(mappedFieldTypesService);
    }

    @Test
    public void returnsEmptyListOnNullRequest() {
        final List<ParsedTerm> result = toTest.identifyUnknownFields(null, Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void returnsEmptyListOnNullTerms() {
        final List<ParsedTerm> result = toTest.identifyUnknownFields(validationRequest, null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void identifiesUnknownFields() {

        final ParsedTerm unknownField = ParsedTerm.create("unknownField", "lalala");
        Collection<ParsedTerm> parsedQueryTerms = Arrays.asList(
                ParsedTerm.create("existingField", "papapa"),
                unknownField,
                ParsedTerm.create(ParsedTerm.DEFAULT_FIELD, "123")
        );

        final List<ParsedTerm> result = toTest.identifyUnknownFields(validationRequest, parsedQueryTerms);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(unknownField));

    }
}
