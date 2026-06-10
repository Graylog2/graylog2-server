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
package org.graylog2.search;

import com.mongodb.MongoClientSettings;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeFieldFiltersTest {

    private static final CodecRegistry CODEC_REGISTRY = MongoClientSettings.getDefaultCodecRegistry();

    private String toJson(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, CODEC_REGISTRY).toJson();
    }

    @Test
    void attributeArrayGeneratesElemMatchForRegex() {
        final SearchQueryField.BsonFilterCreator creator =
                AttributeFieldFilters.attributeArray("host.name");

        final SearchQueryParser.FieldValue fieldValue = new SearchQueryParser.FieldValue(
                "server01", SearchQueryOperators.REGEXP, false);

        final Bson result = creator.createFilter("identifying_attributes", fieldValue);
        final String json = toJson(result);

        assertThat(json).contains("$elemMatch");
        assertThat(json).contains("\"key\": \"host.name\"");
        assertThat(json).contains("$regularExpression");
    }

    @Test
    void attributeArrayGeneratesElemMatchForEquals() {
        final SearchQueryField.BsonFilterCreator creator =
                AttributeFieldFilters.attributeArray("os.type");

        final SearchQueryParser.FieldValue fieldValue = new SearchQueryParser.FieldValue(
                "windows", SearchQueryOperators.EQUALS, false);

        final Bson result = creator.createFilter("non_identifying_attributes", fieldValue);
        final String json = toJson(result);

        assertThat(json).contains("$elemMatch");
        assertThat(json).contains("\"key\": \"os.type\"");
        assertThat(json).contains("\"value\": \"windows\"");
    }

    @Test
    void attributeArrayUsesCorrectArrayField() {
        final SearchQueryField.BsonFilterCreator creator =
                AttributeFieldFilters.attributeArray("service.version");

        final SearchQueryParser.FieldValue fieldValue = new SearchQueryParser.FieldValue(
                "1.0", SearchQueryOperators.REGEXP, false);

        final Bson result = creator.createFilter("non_identifying_attributes", fieldValue);
        final String json = toJson(result);

        assertThat(json).contains("non_identifying_attributes");
    }
}
