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
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.validation.LuceneQueryParser;
import org.graylog.plugins.views.search.validation.QueryValidator;
import org.graylog.plugins.views.search.validation.TestValidationContext;
import org.graylog.plugins.views.search.validation.ValidationContext;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationType;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UnknownFieldsIdentifierTest {

    private QueryValidator sut;

    @BeforeEach
    public void setUp() {
        sut = new UnknownFieldsIdentifier();
    }


    @Test
    void testAllFieldsKnown() {
        final ValidationContext context = TestValidationContext.create("foo: bar OR lorem:ipsum")
                .knownMappedField("foo", "string")
                .knownMappedField("lorem", "string")
                .build();

        final List<ValidationMessage> messages = sut.validate(context);
        assertThat(messages).isEmpty();

    }

    @Test
    public void identifiesUnknownFields() throws InvalidRangeParametersException, ParseException {

        final ValidationContext context = TestValidationContext.create("existingField: papapaa OR unknownField:lalala OR 123")
                .knownMappedField("existingField", "date")
                .build();

        final List<ValidationMessage> result = sut.validate(context);

        assertNotNull(result);
        assertEquals(1, result.size());
        final ValidationMessage unknownTerm = result.iterator().next();
        assertThat(unknownTerm.validationType()).isEqualTo(ValidationType.UNKNOWN_FIELD);
        assertThat(unknownTerm.relatedProperty()).isEqualTo("unknownField");
    }

}
