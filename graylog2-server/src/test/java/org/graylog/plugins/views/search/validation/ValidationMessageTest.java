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

import org.apache.lucene.queryparser.classic.ParseException;
import org.graylog.plugins.views.search.engine.QueryPosition;
import org.graylog.plugins.views.search.validation.validators.ValidationErrors;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ValidationMessageTest {

    private final LuceneQueryParser luceneQueryParser = new LuceneQueryParser();

    @Test
    void fromException() {
        final String query = "foo:";
        try {
            luceneQueryParser.parse(query);
            fail("Should throw an exception!");
        } catch (ParseException e) {
            final List<ValidationMessage> errors = ValidationErrors.create(e);

            assertThat(errors.size()).isEqualTo(1);

            final ValidationMessage validationMessage = errors.iterator().next();

            assertThat(validationMessage.position()).hasValue(QueryPosition.builder()
                    .beginLine(1)
                    .beginColumn(0)
                    .endLine(1)
                    .endColumn(4)
                    .build());
            assertThat(validationMessage.validationType()).isEqualTo(ValidationType.QUERY_PARSING_ERROR);
            assertThat(validationMessage.errorMessage()).startsWith("Cannot parse query, cause: incomplete query, query ended unexpectedly");
        }
    }
}
