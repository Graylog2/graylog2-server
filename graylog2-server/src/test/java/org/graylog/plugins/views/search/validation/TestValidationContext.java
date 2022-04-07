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
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TestValidationContext {

    private final String query;
    private final Set<MappedFieldTypeDTO> fields;
    private TimeRange timeRange;
    private final Set<String> streams;

    private TestValidationContext(String query) {
        this.query = query;
        this.fields = new HashSet<>();
        this.streams = new HashSet<>();
    }

    public static TestValidationContext create(String query) {
        return new TestValidationContext(query);
    }

    public TestValidationContext knownMappedField(final String name, final String type) {
        this.fields.add(MappedFieldTypeDTO.create(name, FieldTypes.Type.builder().type(type).build()));
        return this;
    }

    public TestValidationContext stream(final String streamID) {
        this.streams.add(streamID);
        return this;
    }

    public TestValidationContext timeRange(TimeRange timeRange) {
        this.timeRange = timeRange;
        return this;
    }


    public ValidationContext build() {
        return ValidationContext.builder()
                .query(parseQuery())
                .request(ValidationRequest.builder()
                        .query(ElasticsearchQueryString.of(query))
                        .streams(streams)
                        //.parameters()
                        //.filter()
                        .timerange(getTimeRange())
                        .build())
                .availableFields(fields)
                .build();
    }

    private TimeRange getTimeRange() {
        try {
            return Optional.ofNullable(timeRange).orElse(RelativeRange.create(300));
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException(e);
        }
    }


    private ParsedQuery parseQuery() {
        try {
            return new LuceneQueryParser().parse(query);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
