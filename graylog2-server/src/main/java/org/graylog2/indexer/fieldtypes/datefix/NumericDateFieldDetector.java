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

import jakarta.inject.Inject;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePoller;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds index sets whose write index maps a date field numerically, which is what Graylog 7.1 produced before it
 * was fixed to serialize documents with Graylog's own object mapper.
 * <p>
 * Two signals are used, cheapest first:
 * <ol>
 *     <li>The same field is mapped {@code date} in an older index of the same index set. This needs no query at all
 *     and is conclusive, but it only exists where the installation was upgraded into the affected version.</li>
 *     <li>The values themselves look like epoch timestamps. This costs one field-stats query per candidate field and
 *     is the only signal available on an installation that started out on an affected version, where every index is
 *     numeric and there is nothing to compare against.</li>
 * </ol>
 * Fields the index template defines and fields the user typed deliberately are never considered.
 */
public class NumericDateFieldDetector {
    private static final Logger LOG = LoggerFactory.getLogger(NumericDateFieldDetector.class);

    private static final String DATE_TYPE = "date";
    private static final Set<String> NUMERIC_TYPES =
            Set.of("long", "unsigned_long", "float", "double", "scaled_float");

    /**
     * Numeric fields that the index template maps on purpose, so they can never be a mis-mapped date.
     */
    private static final Set<String> TEMPLATE_NUMERIC_FIELDS = Set.of(
            Message.FIELD_GL2_ACCOUNTED_MESSAGE_SIZE,
            Message.FIELD_GL2_INPUT_MESSAGE_SIZE,
            Message.FIELD_GL2_PROCESSING_DURATION_MS);

    private final IndexFieldTypesService indexFieldTypesService;
    private final IndexFieldTypePoller indexFieldTypePoller;
    private final IndexRangeService indexRangeService;
    private final SearchesAdapter searchesAdapter;
    private final NumericDateFieldClassifier classifier;

    @Inject
    public NumericDateFieldDetector(IndexFieldTypesService indexFieldTypesService,
                                    IndexFieldTypePoller indexFieldTypePoller,
                                    IndexRangeService indexRangeService,
                                    SearchesAdapter searchesAdapter,
                                    NumericDateFieldClassifier classifier) {
        this.indexFieldTypesService = indexFieldTypesService;
        this.indexFieldTypePoller = indexFieldTypePoller;
        this.indexRangeService = indexRangeService;
        this.searchesAdapter = searchesAdapter;
        this.classifier = classifier;
    }

    /**
     * Returns the fields of the given write index that hold dates despite being mapped numerically. An empty result
     * means the index set does not need to be rotated.
     *
     * @param indexSetConfig the index set the write index belongs to
     * @param writeIndex     the index set's active write index
     */
    public List<Finding> findNumericDateFields(IndexSetConfig indexSetConfig, String writeIndex) {
        // The write index is read from the cluster rather than from the cached field types: a field that appeared
        // shortly before the last shutdown may not have been polled into the cache yet, and missing it would leave
        // this index set unrotated while others recover.
        final Optional<IndexFieldTypesDTO> writeIndexTypes =
                indexFieldTypePoller.pollIndex(writeIndex, indexSetConfig.id());
        if (writeIndexTypes.isEmpty()) {
            LOG.debug("Could not read the mapping of write index <{}>, skipping index set <{}>.",
                    writeIndex, indexSetConfig.id());
            return List.of();
        }

        final Set<String> deliberatelyTyped = deliberatelyTypedFields(indexSetConfig);
        // The older indices only serve as a comparison, so the cached types are good enough for them.
        final Map<String, Set<String>> typesInOlderIndices = typesInOlderIndices(
                indexFieldTypesService.findForIndexSet(indexSetConfig.id()), writeIndex);

        final List<FieldTypeDTO> candidates = writeIndexTypes.get().fields().stream()
                .filter(field -> NUMERIC_TYPES.contains(field.physicalType().toLowerCase(Locale.ROOT)))
                .filter(field -> !deliberatelyTyped.contains(field.fieldName()))
                .filter(field -> !TEMPLATE_NUMERIC_FIELDS.contains(field.fieldName()))
                .toList();

        // The free signal first: it needs no query, and naming every affected field makes the log line useful.
        final List<Finding> findings = candidates.stream()
                .filter(field -> typesInOlderIndices
                        .getOrDefault(field.fieldName(), Set.of())
                        .contains(DATE_TYPE))
                .map(field -> new Finding(field.fieldName(), field.physicalType().toLowerCase(Locale.ROOT),
                        Reason.MAPPED_AS_DATE_IN_OLDER_INDEX))
                .collect(Collectors.toCollection(ArrayList::new));

        if (!findings.isEmpty()) {
            return findings;
        }

        // Nothing to compare against, so fall back to reading the values. One hit is enough to decide that this
        // index set has to be rotated, so we stop there instead of querying every remaining field.
        for (FieldTypeDTO field : candidates) {
            final String type = field.physicalType().toLowerCase(Locale.ROOT);
            final Optional<Reason> reason = valuesLookLikeTimestamps(writeIndex, field.fieldName(), type);
            if (reason.isPresent()) {
                findings.add(new Finding(field.fieldName(), type, reason.get()));
                return findings;
            }
        }
        return findings;
    }

    /**
     * Fields whose type the user chose, either directly or through a field type profile. Those are numeric because
     * somebody wanted them to be.
     */
    private Set<String> deliberatelyTypedFields(IndexSetConfig indexSetConfig) {
        if (indexSetConfig.customFieldMappings() == null) {
            return Set.of();
        }
        return indexSetConfig.customFieldMappings().stream()
                .map(CustomFieldMapping::fieldName)
                .collect(Collectors.toSet());
    }

    /**
     * Collects, per field name, the types the index set's other indices use for it.
     */
    private Map<String, Set<String>> typesInOlderIndices(Collection<IndexFieldTypesDTO> indices, String writeIndex) {
        final Map<String, Set<String>> types = new HashMap<>();
        indices.stream()
                .filter(index -> !writeIndex.equals(index.indexName()))
                .flatMap(index -> index.fields().stream())
                .forEach(field -> types
                        .computeIfAbsent(field.fieldName(), name -> new LinkedHashSet<>())
                        .add(field.physicalType().toLowerCase(Locale.ROOT)));
        return types;
    }

    /**
     * Asks the index what the field actually contains. Any failure here is reported as "no finding": a detector that
     * cannot read the values must not cause a rotation.
     */
    private Optional<Reason> valuesLookLikeTimestamps(String index, String fieldName, String physicalType) {
        try {
            final TimeRange allTime = RelativeRange.allTime();
            final var stats = searchesAdapter.fieldStats("*", null, allTime, Set.of(index), fieldName,
                    false, true, true);

            final var verdict = classifier.classify(physicalType, stats.count(), stats.min(), stats.max(),
                    indexPeriod(index));

            return switch (verdict) {
                case EPOCH_MILLIS -> Optional.of(Reason.VALUES_ARE_EPOCH_MILLIS);
                case EPOCH_SECONDS -> Optional.of(Reason.VALUES_ARE_EPOCH_SECONDS);
                case NOT_A_DATE -> Optional.empty();
            };
        } catch (Exception e) {
            LOG.debug("Could not read values of field <{}> in index <{}>, not treating it as a date.",
                    fieldName, index, e);
            return Optional.empty();
        }
    }

    @Nullable
    private NumericDateFieldClassifier.IndexPeriod indexPeriod(String index) {
        try {
            final var range = indexRangeService.get(index);
            return NumericDateFieldClassifier.IndexPeriod.of(range.begin(), range.end());
        } catch (NotFoundException | RuntimeException e) {
            // Without a range we fall back to the plain epoch window.
            return null;
        }
    }

    public enum Reason {
        MAPPED_AS_DATE_IN_OLDER_INDEX("it is mapped as a date in an older index of the same index set"),
        VALUES_ARE_EPOCH_MILLIS("its values are epoch milliseconds"),
        VALUES_ARE_EPOCH_SECONDS("its values are fractional epoch seconds");

        private final String description;

        Reason(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    public record Finding(String fieldName, String physicalType, Reason reason) {
        public String describe() {
            return String.format(Locale.ROOT, "%s (mapped as %s, %s)", fieldName, physicalType, reason.description());
        }
    }
}
