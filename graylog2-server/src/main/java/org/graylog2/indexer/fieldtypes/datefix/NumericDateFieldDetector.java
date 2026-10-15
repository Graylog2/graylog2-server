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
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
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

import static org.graylog2.shared.utilities.StringUtils.f;

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
     * Mappings that whole numbers never produce, so a timestamp-shaped value in one of them can only have come from
     * the fractional epoch seconds that a {@code java.time} value serializes to.
     */
    private static final Set<String> FRACTIONAL_TYPES = Set.of("float", "double", "scaled_float");

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
    private final IndexFieldTypeProfileService profileService;

    @Inject
    public NumericDateFieldDetector(IndexFieldTypesService indexFieldTypesService,
                                    IndexFieldTypePoller indexFieldTypePoller,
                                    IndexRangeService indexRangeService,
                                    SearchesAdapter searchesAdapter,
                                    NumericDateFieldClassifier classifier,
                                    IndexFieldTypeProfileService profileService) {
        this.indexFieldTypesService = indexFieldTypesService;
        this.indexFieldTypePoller = indexFieldTypePoller;
        this.indexRangeService = indexRangeService;
        this.searchesAdapter = searchesAdapter;
        this.classifier = classifier;
        this.profileService = profileService;
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
            // Not knowing the mapping is not the same as knowing there is nothing to repair. Reporting this as a
            // failure keeps the migration eligible to run again instead of marking the index set as done.
            throw new IllegalStateException(f("Could not read the mapping of write index <%s>", writeIndex));
        }

        final Map<String, String> configuredTypes = configuredFieldTypes(indexSetConfig);
        // The older indices only serve as a comparison, so the cached types are good enough for them.
        final Map<String, Set<String>> typesInOlderIndices = typesInOlderIndices(
                indexFieldTypesService.findForIndexSet(indexSetConfig.id()), writeIndex);

        final List<FieldTypeDTO> candidates = writeIndexTypes.get().fields().stream()
                .filter(field -> NUMERIC_TYPES.contains(field.physicalType().toLowerCase(Locale.ROOT)))
                // A field that is configured as something other than a date is numeric on purpose.
                .filter(field -> !configuredTypes.containsKey(field.fieldName())
                        || DATE_TYPE.equals(configuredTypes.get(field.fieldName())))
                .filter(field -> !TEMPLATE_NUMERIC_FIELDS.contains(field.fieldName()))
                .toList();

        // Every candidate is classified, so that each affected field can be pinned. Leaving one out would hand it
        // back to dynamic mapping, where the next value that arrives decides its type again.
        final List<Finding> findings = new ArrayList<>();
        for (FieldTypeDTO field : candidates) {
            final String type = field.physicalType().toLowerCase(Locale.ROOT);

            if (DATE_TYPE.equals(configuredTypes.get(field.fieldName()))) {
                // Configured as a date, yet this index maps it numerically: the rotation that would have applied the
                // configuration has not happened yet, most likely because an earlier attempt failed after the
                // mapping was stored.
                findings.add(new Finding(field.fieldName(), type, Reason.CONFIGURED_AS_DATE_BUT_MAPPED_NUMERICALLY));
                continue;
            }

            if (typesInOlderIndices.getOrDefault(field.fieldName(), Set.of()).contains(DATE_TYPE)) {
                findings.add(new Finding(field.fieldName(), type, Reason.MAPPED_AS_DATE_IN_OLDER_INDEX));
                continue;
            }

            // Neither free signal applies, so the only evidence left is the values themselves. That is conclusive
            // for a floating point mapping - whole numbers never produce one, so it can only have come from the
            // fractional epoch seconds a java.time value serializes to - but not for an integer mapping, where a
            // large counter is indistinguishable from epoch milliseconds. Retyping such a field would break a
            // perfectly healthy installation, so an integer field is only repaired on independent evidence.
            if (FRACTIONAL_TYPES.contains(type)) {
                valuesLookLikeTimestamps(writeIndex, field.fieldName(), type)
                        .ifPresent(reason -> findings.add(new Finding(field.fieldName(), type, reason)));
            }
        }
        return findings;
    }

    /**
     * Fields whose type the user chose, either directly or through a field type profile. Those are numeric because
     * somebody wanted them to be.
     */
    private Map<String, String> configuredFieldTypes(IndexSetConfig indexSetConfig) {
        final Map<String, String> types = new HashMap<>();

        // A profile's mappings are stored separately and merged into the template, so they are just as deliberate as
        // the ones on the index set itself. The index set's own mappings win, as they do in the template.
        final String profileId = indexSetConfig.fieldTypeProfile();
        if (profileId != null && !profileId.isEmpty()) {
            profileService.get(profileId)
                    .map(IndexFieldTypeProfile::customFieldMappings)
                    .ifPresent(mappings -> mappings.forEach(
                            mapping -> types.put(mapping.fieldName(), mapping.type().toLowerCase(Locale.ROOT))));
        }

        if (indexSetConfig.customFieldMappings() != null) {
            indexSetConfig.customFieldMappings().forEach(
                    mapping -> types.put(mapping.fieldName(), mapping.type().toLowerCase(Locale.ROOT)));
        }

        return types;
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
     * Asks the index what the field actually contains. A failure is propagated rather than read as "no finding":
     * not being able to judge a field is not the same as knowing that it is fine, and only a failure keeps the
     * migration eligible to try again.
     */
    private Optional<Reason> valuesLookLikeTimestamps(String index, String fieldName, String physicalType) {
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
    }

    @Nullable
    private NumericDateFieldClassifier.IndexPeriod indexPeriod(String index) {
        try {
            final var range = indexRangeService.get(index);
            if (range.begin().getMillis() == 0 && range.end().getMillis() == 0) {
                // The range of an active write index is a placeholder that is only filled in once the index is set
                // read-only, see MongoIndexSet#addDeflectorIndexRange. Taking it literally would put the index in
                // 1970 and reject every realistic timestamp.
                return null;
            }
            return NumericDateFieldClassifier.IndexPeriod.of(range.begin(), range.end());
        } catch (NotFoundException | RuntimeException e) {
            // Without a range we fall back to the plain epoch window.
            return null;
        }
    }

    public enum Reason {
        CONFIGURED_AS_DATE_BUT_MAPPED_NUMERICALLY("it is configured as a date but this index still maps it numerically"),
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
            return f("%s (mapped as %s, %s)", fieldName, physicalType, reason.description());
        }
    }
}
