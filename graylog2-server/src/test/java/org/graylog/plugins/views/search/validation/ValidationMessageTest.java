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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ValidationMessageTest {

    private final LuceneQueryParser luceneQueryParser = new LuceneQueryParser(false);

    @Test
    void fromException() {
        final String query = "foo:";
        try {
            luceneQueryParser.parse(query);
            fail("Should throw an exception!");
        } catch (ParseException e) {
            final ValidationMessage validationMessage = ValidationMessage.fromException( e);
            assertThat(validationMessage.beginLine()).isEqualTo(1);
            assertThat(validationMessage.endLine()).isEqualTo(1);
            assertThat(validationMessage.beginColumn()).isEqualTo(0);
            assertThat(validationMessage.endColumn()).isEqualTo(4);
            assertThat(validationMessage.validationType()).isEqualTo(ValidationType.QUERY_PARSING_ERROR);
            assertThat(validationMessage.errorMessage()).startsWith("Cannot parse query, cause: incomplete query, query ended unexpectedly");
        }
    }
}
