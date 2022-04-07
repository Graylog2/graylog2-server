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

import org.apache.lucene.queryparser.classic.ParseException;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.validation.LuceneQueryParser;
import org.graylog.plugins.views.search.validation.ParsedQuery;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationRequest;
import org.graylog.plugins.views.search.validation.ValidationType;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnknownFieldsIdentifierTest {

    private UnknownFieldsIdentifier sut;
    private LuceneQueryParser parser;

    @BeforeEach
    public void setUp() {
        MappedFieldTypesService mappedFieldTypesService = (streamIds, timeRange) -> Collections.singleton(MappedFieldTypeDTO.create("existingField", FieldTypes.Type.builder().type("date").build()));
        sut = new UnknownFieldsIdentifier(mappedFieldTypesService);
        parser = new LuceneQueryParser();
    }

    @Test
    public void returnsEmptyListOnNullRequest() throws ParseException {
        final List<ValidationMessage> result = sut.validate(null, parser.parse("foo:bar"));
        assertTrue(result.isEmpty());
    }

    @Test
    public void returnsEmptyListOnNullQuery() throws InvalidRangeParametersException {
        final List<ValidationMessage> result = sut.validate(validationRequest(), null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void identifiesUnknownFields() throws InvalidRangeParametersException, ParseException {

        final ParsedQuery query = parser.parse("existingField: papapaa OR unknownField:lalala OR 123");

        final List<ValidationMessage> result = sut.validate(validationRequest(), query);
        assertNotNull(result);
        assertEquals(1, result.size());
        final ValidationMessage unknownTerm = result.iterator().next();
        assertThat(unknownTerm.validationType()).isEqualTo(ValidationType.UNKNOWN_FIELD);
        assertThat(unknownTerm.relatedProperty()).isEqualTo("unknownField");
    }

    private ValidationRequest validationRequest() throws InvalidRangeParametersException {
        return ValidationRequest.builder()
                .query(ElasticsearchQueryString.of(""))
                .timerange(RelativeRange.create(300))
                .streams(Collections.emptySet())
                .build();
    }
}
