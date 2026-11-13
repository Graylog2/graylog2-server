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
import org.graylog2.indexer.results.FieldStatsResult;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.plugin.Message;
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
    private final SearchesAdapter searchesAdapter = mock(SearchesAdapter.class);
    private final IndexFieldTypeProfileService profileService = mock(IndexFieldTypeProfileService.class);

    private NumericDateFieldDetector detector;

    @BeforeEach
    void setUp() {
        detector = new NumericDateFieldDetector(indexFieldTypesService, indexFieldTypePoller,
                searchesAdapter, new NumericDateFieldClassifier(), profileService);
        // every index the tests look at was written around the timestamps they use
        givenFieldStats(Message.FIELD_TIMESTAMP, 100, EPOCH_MILLIS);
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
        // the conflict is conclusive, so this field's values never have to be read
        verify(searchesAdapter, never()).fieldStats(anyString(), any(), any(), any(), eq("event_start"),
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
    void ignoresFieldsWhoseTypeCannotBeChanged() {
        // Pinning one of these is rejected by CustomMappingValidation, so reporting it would leave the index set
        // unfinished and rotate it again on every startup. They would otherwise be found by value, so the assertion
        // really does exercise the filter.
        givenIndices(index(WRITE_INDEX,
                field(Message.FIELD_GL2_ACCOUNTED_MESSAGE_SIZE, "long"),
                field(Message.FIELD_GL2_INPUT_MESSAGE_SIZE, "long"),
                field(Message.FIELD_GL2_REMOTE_PORT, "long")));
        givenFieldStats(Message.FIELD_GL2_ACCOUNTED_MESSAGE_SIZE, 10, EPOCH_MILLIS);
        givenFieldStats(Message.FIELD_GL2_INPUT_MESSAGE_SIZE, 10, EPOCH_MILLIS);
        givenFieldStats(Message.FIELD_GL2_REMOTE_PORT, 10, EPOCH_MILLIS);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX)).isEmpty();
    }

    @Test
    void ignoresEveryFieldThatCustomMappingValidationWouldReject() {
        // the filter has to stay in step with the validation, so it is checked against the same list
        Message.FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS.forEach(fieldName -> {
            givenIndices(index(WRITE_INDEX, field(fieldName, "long")));
            givenFieldStats(fieldName, 10, EPOCH_MILLIS);

            assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                    .as("field %s must not be reported", fieldName)
                    .isEmpty();
        });
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
    void skipsAnIndexSetWhoseWriteIndexDoesNotExist() {
        // the poller reports an empty result only for a missing index, and retrying would not make it appear
        givenIndices(index(OLDER_INDEX, field("event_start", "date")));

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX)).isEmpty();
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
        givenFieldStats("event_start", 10, EPOCH_SECONDS);
        givenFieldStats("event_end", 10, EPOCH_SECONDS);

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
                .isInstanceOf(RuntimeException.class)
                .hasMessage("index is closed");
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
    void findsAnIntegerFieldWhoseValuesAgreeWithTheIndexPeriod() {
        givenIndices(index(WRITE_INDEX, field("event_start", "long")));
        givenFieldStats("event_start", 10, EPOCH_MILLIS);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .singleElement()
                .satisfies(finding -> assertThat(finding.reason())
                        .isEqualTo(NumericDateFieldDetector.Reason.VALUES_ARE_EPOCH_MILLIS));
    }

    @Test
    void ignoresACounterThatLandsInTheEpochWindowButNotInTheIndexPeriod() {
        // ~1.5 TB in bytes reads as a 2017 timestamp, while this index was written in 2025
        givenIndices(index(WRITE_INDEX, field("bytes_total", "long")));
        givenFieldStats("bytes_total", 10, 1_500_000_000_000d);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX)).isEmpty();
    }

    @Test
    void treatsAnEmptyWriteIndexAsNothingToDo() {
        // Every index set has an empty write index right after a rotation, and a newly created one has nothing else.
        // The faulty serialization needs a document to happen, so an empty index cannot hold a mis-mapped field.
        givenIndices(index(WRITE_INDEX, field("event_start", "long")));
        givenFieldStats(Message.FIELD_TIMESTAMP, 0, Double.NaN);
        givenFieldStats("event_start", 10, EPOCH_MILLIS);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX)).isEmpty();
    }

    @Test
    void failsWhenAnIndexWithMessagesHasNoUsableTimestampRange() {
        // without the period there is nothing to check a value against, and "cannot tell" must not look like "fine"
        givenIndices(index(WRITE_INDEX, field("event_start", "long")));
        givenFieldStats(Message.FIELD_TIMESTAMP, 10, Double.NaN);
        givenFieldStats("event_start", 10, EPOCH_MILLIS);

        assertThatThrownBy(() -> detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(WRITE_INDEX);
    }

    @Test
    void keepsTheFindingsOfTheFreeSignalsWhenTheWriteIndexIsEmpty() {
        // the older index proves event_start was a date; counter is only a guess and cannot be checked without data
        givenIndices(
                index(WRITE_INDEX, field("event_start", "long"), field("counter", "long")),
                index(OLDER_INDEX, field("event_start", "date")));
        givenFieldStats(Message.FIELD_TIMESTAMP, 0, Double.NaN);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX))
                .extracting(NumericDateFieldDetector.Finding::fieldName)
                .containsExactly("event_start");
    }

    @Test
    void doesNotReadTheIndexPeriodWhenEveryFieldIsSettledByAFreeSignal() {
        givenIndices(
                index(WRITE_INDEX, field("event_start", "long")),
                index(OLDER_INDEX, field("event_start", "date")));

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX)).hasSize(1);
        verify(searchesAdapter, never()).fieldStats(anyString(), any(), any(), any(), eq(Message.FIELD_TIMESTAMP),
                anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void ignoresACounterThatSpansTheIndexPeriodFromBelow() {
        // a counter running from 1.0 to 1.9 trillion overlaps the period without its values being near it
        givenIndices(index(WRITE_INDEX, field("bytes_total", "long")));
        final FieldStatsResult spanning = mock(FieldStatsResult.class);
        when(spanning.count()).thenReturn(10L);
        when(spanning.min()).thenReturn(1_000_000_000_000d);
        when(spanning.max()).thenReturn(1_900_000_000_000d);
        when(searchesAdapter.fieldStats(anyString(), any(), any(), any(), eq("bytes_total"),
                anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(spanning);

        assertThat(detector.findNumericDateFields(indexSetConfig(), WRITE_INDEX)).isEmpty();
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

    /**
     * The first index given is the write index, which the detector reads live; the rest are the older indices it
     * compares against, which come from the cached field types.
     */
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
