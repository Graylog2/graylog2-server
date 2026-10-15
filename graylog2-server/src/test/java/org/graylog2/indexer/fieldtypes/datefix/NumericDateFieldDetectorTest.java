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
package org.graylog2.indexer.fieldtypes.datefix;

import com.google.common.collect.ImmutableSet;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePoller;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NumericDateFieldDetectorTest {
    private static final String INDEX_SET_ID = "index-set-1";
    private static final String WRITE_INDEX = "graylog_42";
    private static final String OLDER_INDEX = "graylog_41";
    private static final double EPOCH_MILLIS = 1_758_528_672_000d;
    private static final double EPOCH_SECONDS = 1_758_528_672d;

    private final IndexFieldTypesService indexFieldTypesService = mock(IndexFieldTypesService.class);
    private final IndexFieldTypePoller indexFieldTypePoller = mock(IndexFieldTypePoller.class);
    private final IndexRangeService indexRangeService = mock(IndexRangeService.class);
    private final SearchesAdapter searchesAdapter = mock(SearchesAdapter.class);
    private final IndexFieldTypeProfileService profileService = mock(IndexFieldTypeProfileService.class);

    private NumericDateFieldDetector detector;

    @BeforeEach
    void setUp() {
        detector = new NumericDateFieldDetector(indexFieldTypesService, indexFieldTypePoller, indexRangeService,
                searchesAdapter, new NumericDateFieldClassifier(), profileService);
    }

    @Test
    void findsAFieldThatIsStillADateInAnOlderIndex() {
        givenIndices(
                index(WRITE_INDEX, field("event_start", "long")),
                index(OLDER_INDEX, field("event_start", "date")));

        final var findings = detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX);

        assertThat(findings).singleElement().satisfies(finding -> {
            assertThat(finding.fieldName()).isEqualTo("event_start");
            assertThat(finding.reason()).isEqualTo(NumericDateFieldDetector.Reason.MAPPED_AS_DATE_IN_OLDER_INDEX);
        });
        // the conflict is conclusive, so the values never have to be read
        verify(searchesAdapter, never()).fieldStats(anyString(), any(), any(), any(), anyString(),
                anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void findsAFieldByItsValuesWhenThereIsNothingToCompareAgainst() {
        // a fresh installation: every index was written by the affected version, so no index maps the field as a date
        givenIndices(index(WRITE_INDEX, field("event_start", "float")));
        givenFieldStats("event_start", 10, EPOCH_SECONDS);

        final var findings = detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX);

        assertThat(findings).singleElement().satisfies(finding ->
                assertThat(finding.reason()).isEqualTo(NumericDateFieldDetector.Reason.VALUES_ARE_EPOCH_SECONDS));
    }

    @Test
    void ignoresOrdinaryNumericFields() {
        givenIndices(index(WRITE_INDEX, field("took_ms", "long")));
        givenFieldStats("took_ms", 10, 42d);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX)).isEmpty();
    }

    @Test
    void ignoresFieldsTheIndexTemplateTypes() {
        givenIndices(index(WRITE_INDEX,
                field(Message.FIELD_GL2_ACCOUNTED_MESSAGE_SIZE, "long"),
                field(Message.FIELD_GL2_PROCESSING_DURATION_MS, "integer")));

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX)).isEmpty();
        verify(searchesAdapter, never()).fieldStats(anyString(), any(), any(), any(), anyString(),
                anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void ignoresFieldsTheUserTypedDeliberately() {
        givenIndices(
                index(WRITE_INDEX, field("event_start", "long")),
                index(OLDER_INDEX, field("event_start", "date")));

        final IndexSetConfig config = indexSetConfig(new CustomFieldMappings(
                List.of(new CustomFieldMapping("event_start", "long"))));

        assertThat(detector.findNumericDateFields(config, WRITE_INDEX)).isEmpty();
    }

    @Test
    void ignoresNonNumericFields() {
        givenIndices(index(WRITE_INDEX, field("message", "text"), field("timestamp", "date")));

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX)).isEmpty();
    }

    @Test
    void failsWhenTheWriteIndexMappingCannotBeRead() {
        givenIndices(index(OLDER_INDEX, field("event_start", "date")));

        // not knowing the mapping must not look like "nothing to repair", or the migration would mark itself done
        assertThatThrownBy(() -> detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(WRITE_INDEX);
    }

    @Test
    void readsTheWriteIndexMappingFromTheClusterRatherThanTheCache() {
        // a field polled into the write index after the cache was last written still has to be found
        when(indexFieldTypesService.findForIndexSet(INDEX_SET_ID)).thenReturn(List.of());
        when(indexFieldTypePoller.pollIndex(WRITE_INDEX, INDEX_SET_ID))
                .thenReturn(Optional.of(index(WRITE_INDEX, field("event_start", "float"))));
        givenFieldStats("event_start", 10, EPOCH_SECONDS);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .extracting(NumericDateFieldDetector.Finding::fieldName)
                .containsExactly("event_start");
    }

    @Test
    void reportsEveryAffectedFieldFoundByItsValues() {
        givenIndices(index(WRITE_INDEX, field("event_start", "float"), field("event_end", "float")));
        final FieldStatsResult stats = statsOf(10, EPOCH_SECONDS);
        when(searchesAdapter.fieldStats(anyString(), any(), any(), any(), anyString(),
                anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(stats);

        // every affected field has to be named, otherwise it is not pinned and dynamic mapping decides it again
        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .extracting(NumericDateFieldDetector.Finding::fieldName)
                .containsExactlyInAnyOrder("event_start", "event_end");
    }

    @Test
    void ignoresFieldsTypedByTheIndexSetsFieldTypeProfile() {
        givenIndices(
                index(WRITE_INDEX, field("event_start", "long")),
                index(OLDER_INDEX, field("event_start", "date")));

        final IndexFieldTypeProfile profile = mock(IndexFieldTypeProfile.class);
        when(profile.customFieldMappings())
                .thenReturn(new CustomFieldMappings(List.of(new CustomFieldMapping("event_start", "long"))));
        when(profileService.get("profile-1")).thenReturn(Optional.of(profile));

        final IndexSetConfig config = indexSetConfig();
        when(config.fieldTypeProfile()).thenReturn("profile-1");

        assertThat(detector.findNumericDateFields(config, WRITE_INDEX)).isEmpty();
    }

    @Test
    void failsWhenTheValuesCannotBeRead() {
        givenIndices(index(WRITE_INDEX, field("event_start", "float")));
        when(searchesAdapter.fieldStats(anyString(), any(), any(), any(), eq("event_start"),
                anyBoolean(), anyBoolean(), anyBoolean()))
                .thenThrow(new RuntimeException("index is closed"));

        // being unable to judge a field is not the same as knowing it is fine
        assertThatThrownBy(() -> detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void findsEveryAffectedFieldOfTheWriteIndex() {
        givenIndices(
                index(WRITE_INDEX, field("event_start", "long"), field("event_end", "long"), field("took_ms", "long")),
                index(OLDER_INDEX, field("event_start", "date"), field("event_end", "date")));
        givenFieldStats("took_ms", 10, 42d);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .extracting(NumericDateFieldDetector.Finding::fieldName)
                .containsExactlyInAnyOrder("event_start", "event_end");
    }

    /**
     * The first index given is the write index, which the detector reads live; the rest are the older indices it
     * compares against, which come from the cached field types.
     */

    @Test
    void findsBothHistoricallyConfirmedAndValueDetectedFieldsInTheSameIndex() {
        givenIndices(
                index(WRITE_INDEX, field("event_start", "long"), field("event_end", "float")),
                index(OLDER_INDEX, field("event_start", "date")));
        // only event_end needs a value query, but both have to be reported so that both get pinned
        givenFieldStats("event_end", 10, EPOCH_SECONDS);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .extracting(NumericDateFieldDetector.Finding::fieldName)
                .containsExactlyInAnyOrder("event_start", "event_end");
    }

    @Test
    void findsAFieldThatIsConfiguredAsDateButStillMappedNumerically() {
        // an earlier repair stored the mapping and then failed to rotate: the work is not done
        givenIndices(index(WRITE_INDEX, field("event_start", "long")));

        final IndexSetConfig config = indexSetConfig(new CustomFieldMappings(
                List.of(new CustomFieldMapping("event_start", "date"))));

        assertThat(detector.findNumericDateFields(config, WRITE_INDEX))
                .singleElement()
                .satisfies(finding -> assertThat(finding.reason())
                        .isEqualTo(NumericDateFieldDetector.Reason.CONFIGURED_AS_DATE_BUT_MAPPED_NUMERICALLY));
    }


    @Test
    void stillDetectsValuesWhenTheIndexRangeIsTheWriteIndexPlaceholder() throws Exception {
        // An active write index has a placeholder range of epoch zero until it is set read-only. Taken literally it
        // would put the index in 1970 and reject every realistic timestamp.
        givenIndices(index(WRITE_INDEX, field("event_start", "float")));
        givenFieldStats("event_start", 10, EPOCH_SECONDS);

        final IndexRange placeholder = mock(IndexRange.class);
        when(placeholder.begin()).thenReturn(new DateTime(0L, DateTimeZone.UTC));
        when(placeholder.end()).thenReturn(new DateTime(0L, DateTimeZone.UTC));
        when(indexRangeService.get(WRITE_INDEX)).thenReturn(placeholder);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .extracting(NumericDateFieldDetector.Finding::fieldName)
                .containsExactly("event_start");
    }


    @Test
    void doesNotRetypeAnIntegerFieldOnValuesAlone() {
        // a byte counter is indistinguishable from epoch milliseconds, and retyping it would break a healthy
        // installation that never had the serializer problem
        givenIndices(index(WRITE_INDEX, field("bytes_total", "long")));
        givenFieldStats("bytes_total", 10, EPOCH_MILLIS);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX)).isEmpty();
        verify(searchesAdapter, never()).fieldStats(anyString(), any(), any(), any(), anyString(),
                anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void stillRepairsAnIntegerFieldWhenAnOlderIndexConfirmsIt() {
        givenIndices(
                index(WRITE_INDEX, field("event_start", "long")),
                index(OLDER_INDEX, field("event_start", "date")));

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .extracting(NumericDateFieldDetector.Finding::fieldName)
                .containsExactly("event_start");
    }

    private void givenIndices(IndexFieldTypesDTO... indices) {
        when(indexFieldTypesService.findForIndexSet(INDEX_SET_ID)).thenReturn(List.of(indices));
        final Optional<IndexFieldTypesDTO> writeIndexTypes = List.of(indices).stream()
                .filter(index -> WRITE_INDEX.equals(index.indexName()))
                .findFirst();
        when(indexFieldTypePoller.pollIndex(WRITE_INDEX, INDEX_SET_ID)).thenReturn(writeIndexTypes);
    }

    private void givenFieldStats(String field, long count, double value) {
        final FieldStatsResult stats = statsOf(count, value);
        when(searchesAdapter.fieldStats(anyString(), any(), any(), any(), eq(field),
                anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(stats);
    }

    private static FieldStatsResult statsOf(long count, double value) {
        final FieldStatsResult stats = mock(FieldStatsResult.class);
        when(stats.count()).thenReturn(count);
        when(stats.min()).thenReturn(value);
        when(stats.max()).thenReturn(value);
        return stats;
    }

    private static IndexFieldTypesDTO index(String indexName, FieldTypeDTO... fields) {
        return IndexFieldTypesDTO.builder()
                .indexSetId(INDEX_SET_ID)
                .indexName(indexName)
                .fields(ImmutableSet.copyOf(fields))
                .build();
    }

    private static FieldTypeDTO field(String name, String physicalType) {
        return FieldTypeDTO.create(name, physicalType);
    }

    private static IndexSetConfig indexSetConfig() {
        return indexSetConfig(new CustomFieldMappings());
    }

    private static IndexSetConfig indexSetConfig(CustomFieldMappings customFieldMappings) {
        final IndexSetConfig config = mock(IndexSetConfig.class);
        when(config.id()).thenReturn(INDEX_SET_ID);
        when(config.customFieldMappings()).thenReturn(customFieldMappings);
        return config;
    }
}
