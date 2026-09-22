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
package org.graylog2.indexer.messages;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MappingErrorCoercionTest {

    private static final DateTime TIMESTAMP = new DateTime(2026, 9, 21, 10, 0, 0, 0, DateTimeZone.UTC);

    /**
     * The shape the indexer actually reports, wrapped the way the storage adapters hand it on.
     */
    private static String mappingError(String field, String type) {
        return "OpenSearchException[OpenSearch exception [type=mapper_parsing_exception, reason=failed to parse "
                + "field [" + field + "] of type [" + type + "] in document with id '01K5X'. "
                + "Preview of field's value: '2026-09-21T10:00:00.000Z']];";
    }

    @Test
    void findsFieldMappedToNumericType() {
        assertThat(MappingErrorCoercion.numericFieldFrom(mappingError("event_start", "long")))
                .contains("event_start");
        assertThat(MappingErrorCoercion.numericFieldFrom(mappingError("ratio", "double")))
                .contains("ratio");
    }

    @Test
    void findsFieldMappedToFloatingPointType() {
        assertThat(MappingErrorCoercion.numericFieldFrom(mappingError("event_start", "float")))
                .contains("event_start");
    }

    @Test
    void ignoresFieldsMappedToNonNumericTypes() {
        assertThat(MappingErrorCoercion.numericFieldFrom(mappingError("event_start", "date"))).isEmpty();
        assertThat(MappingErrorCoercion.numericFieldFrom(mappingError("event_start", "keyword"))).isEmpty();
    }

    @Test
    void ignoresUnrelatedErrors() {
        assertThat(MappingErrorCoercion.numericFieldFrom(null)).isEmpty();
        assertThat(MappingErrorCoercion.numericFieldFrom("")).isEmpty();
        assertThat(MappingErrorCoercion.numericFieldFrom(
                "object mapping for [foo] tried to parse field as object, but found a concrete value")).isEmpty();
    }

    @Test
    void doesNotCoerceWhenErrorIsNotAboutANumericField() {
        final Indexable message = messageWith("event_start", TIMESTAMP);

        assertThat(MappingErrorCoercion.coerce(message, mappingError("event_start", "date"))).isEmpty();
    }

    @Test
    void writesDateTimeValueAsEpochMillisForIntegerMappedFields() {
        final Indexable coerced = coerce(messageWith("event_start", TIMESTAMP), "event_start");

        assertThat(sourceOf(coerced)).containsEntry("event_start", TIMESTAMP.getMillis());
    }

    @Test
    void writesDateStringValueAsEpochMillis() {
        final Indexable coerced = coerce(messageWith("event_start", "2026-09-21T10:00:00.000Z"), "event_start");

        assertThat(sourceOf(coerced)).containsEntry("event_start", TIMESTAMP.getMillis());
    }

    @Test
    void leavesValuesThatAreNotDatesUntouched() {
        final Indexable coerced = coerce(messageWith("event_start", "not a date"), "event_start");

        assertThat(sourceOf(coerced)).containsEntry("event_start", "not a date");
    }

    @Test
    void leavesTheRemainingDocumentUntouched() {
        final Indexable message = new StubIndexable(Map.of(
                "event_start", TIMESTAMP,
                "message", "hello",
                "timestamp", "2026-09-21 10:00:00.000"));

        final Map<String, Object> source = sourceOf(coerce(message, "event_start"));

        assertThat(source)
                .containsEntry("event_start", TIMESTAMP.getMillis())
                .containsEntry("message", "hello")
                .containsEntry("timestamp", "2026-09-21 10:00:00.000");
        // the original document must not be modified in place
        assertThat(sourceOf(message)).containsEntry("event_start", TIMESTAMP);
    }

    @Test
    void ignoresFieldsThatAreNotPresentInTheDocument() {
        final Indexable coerced = coerce(messageWith("event_start", TIMESTAMP), "some_other_field");

        assertThat(sourceOf(coerced)).containsEntry("event_start", TIMESTAMP);
    }


    @Test
    void writesDateValueAsEpochSecondsForFloatMappedFields() {
        // grok's ;date; conversion produces a java.time.Instant, which was written as fractional epoch seconds,
        // so such a field is mapped as float and expects the same shape now
        final Indexable coerced = coerce(messageWith("event_start", TIMESTAMP), "event_start", "float");

        assertThat(sourceOf(coerced))
                .containsEntry("event_start", BigDecimal.valueOf(TIMESTAMP.getMillis(), 3));
    }

    @Test
    void writesInstantValueAsEpochSecondsForDoubleMappedFields() {
        final Indexable message = messageWith("event_start", Instant.ofEpochMilli(TIMESTAMP.getMillis()));

        final Indexable coerced = coerce(message, "event_start", "double");

        assertThat(sourceOf(coerced))
                .containsEntry("event_start", BigDecimal.valueOf(TIMESTAMP.getMillis(), 3));
    }

    private static Indexable coerce(Indexable message, String field) {
        return coerce(message, field, "long");
    }

    private static Indexable coerce(Indexable message, String field, String type) {
        final Optional<Indexable> coerced = MappingErrorCoercion.coerce(message, mappingError(field, type));
        assertThat(coerced).isPresent();
        return coerced.get();
    }

    private static Indexable messageWith(String field, Object value) {
        return new StubIndexable(Map.of(field, value));
    }

    private static Map<String, Object> sourceOf(Indexable message) {
        return message.toElasticSearchObject(new ObjectMapper(), new Meter());
    }

    private record StubIndexable(Map<String, Object> source) implements Indexable {
        @Override
        public String getId() {
            return "01K5X";
        }

        @Override
        public String getMessageId() {
            return "01K5X";
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public DateTime getReceiveTime() {
            return TIMESTAMP;
        }

        @Override
        public Map<String, Object> toElasticSearchObject(ObjectMapper objectMapper, @Nonnull Meter invalidTimestampMeter) {
            return source;
        }

        @Override
        public DateTime getTimestamp() {
            return TIMESTAMP;
        }
    }
}
