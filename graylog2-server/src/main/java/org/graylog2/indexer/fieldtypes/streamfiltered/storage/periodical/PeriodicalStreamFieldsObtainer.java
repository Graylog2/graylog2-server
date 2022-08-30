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
package org.graylog2.indexer.fieldtypes.streamfiltered.storage.periodical;

import com.google.common.collect.Iterables;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog2.Configuration;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.AggregationBasedFieldTypeFilterAdapter;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.CountExistingBasedFieldTypeFilterAdapter;
import org.graylog2.indexer.fieldtypes.streamfiltered.storage.StoredStreamFieldsService;
import org.graylog2.indexer.fieldtypes.streamfiltered.storage.model.StoredStreamFields;
import org.graylog2.indexer.fieldtypes.util.TextFieldTypesSeparator;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.indexer.fieldtypes.streamfiltered.config.Config.MAX_AGGREGATIONS_PER_REQUEST;
import static org.graylog2.indexer.fieldtypes.streamfiltered.config.Config.MAX_SEARCHES_PER_MULTI_SEARCH;

public class PeriodicalStreamFieldsObtainer extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodicalStreamFieldsObtainer.class);

    private final StoredStreamFieldsService storedStreamFieldsService;
    private final AggregationBasedFieldTypeFilterAdapter aggregationBasedFieldTypeFilterAdapter;
    private final CountExistingBasedFieldTypeFilterAdapter countExistingBasedFieldTypeFilterAdapter;
    private final StreamService streamService;
    private final IndexFieldTypesService indexFieldTypesService;
    private final IndexLookup indexLookup;
    private final boolean maintainsStreamBasedFieldLists;

    @Inject
    public PeriodicalStreamFieldsObtainer(final Configuration configuration,
                                          final StreamService streamService,
                                          final IndexFieldTypesService indexFieldTypesService,
                                          final IndexLookup indexLookup,
                                          final StoredStreamFieldsService storedStreamFieldsService,
                                          final AggregationBasedFieldTypeFilterAdapter aggregationBasedFieldTypeFilterAdapter,
                                          final CountExistingBasedFieldTypeFilterAdapter countExistingBasedFieldTypeFilterAdapter) {
        this.storedStreamFieldsService = storedStreamFieldsService;
        this.aggregationBasedFieldTypeFilterAdapter = aggregationBasedFieldTypeFilterAdapter;
        this.countExistingBasedFieldTypeFilterAdapter = countExistingBasedFieldTypeFilterAdapter;
        this.streamService = streamService;
        this.indexFieldTypesService = indexFieldTypesService;
        this.indexLookup = indexLookup;
        this.maintainsStreamBasedFieldLists = configuration.maintainsStreamBasedFieldLists();
    }

    @Override
    public void doRun() {
        if (!maintainsStreamBasedFieldLists) {
            return;
        }
        final List<Stream> allStreams = streamService.loadAll();
        for (Stream stream : allStreams) {
            try {
                final String streamId = stream.getId();
                final Set<FieldTypeDTO> fieldTypeDTOsForStream = getProperFieldTypesForStream(streamId);
                storedStreamFieldsService.save(StoredStreamFields.create(streamId, fieldTypeDTOsForStream));
            } catch (Exception e) {
                LOG.error("Failed to obtain and store field types for stream : " + stream.getId(), e);
            }
        }
    }

    private Set<FieldTypeDTO> getProperFieldTypesForStream(final String streamId) {
        final Set<String> streamIdSingleton = Collections.singleton(streamId);
        final Set<String> indexNames = this.indexLookup.indexNamesForStreamsInTimeRange(streamIdSingleton,
                RelativeRange.allTime());
        final Set<FieldTypeDTO> initialFieldTypeDTOs = getInitialFieldTypes(streamId, indexNames);

        final TextFieldTypesSeparator textFieldTypesSeparator = new TextFieldTypesSeparator();
        textFieldTypesSeparator.separate(initialFieldTypeDTOs);

        final Set<FieldTypeDTO> fieldTypeDTOsForStream = new HashSet<>();
        final Iterable<List<FieldTypeDTO>> textFieldsBatches = Iterables.partition(textFieldTypesSeparator.getTextFields(), MAX_SEARCHES_PER_MULTI_SEARCH);
        for (List<FieldTypeDTO> batch : textFieldsBatches) {
            final Set<FieldTypeDTO> filteredBatch = countExistingBasedFieldTypeFilterAdapter.filterFieldTypes(new HashSet<>(batch), indexNames, streamIdSingleton);
            fieldTypeDTOsForStream.addAll(filteredBatch);
        }
        final Iterable<List<FieldTypeDTO>> nonTextFieldsBatches = Iterables.partition(textFieldTypesSeparator.getNonTextFields(), MAX_AGGREGATIONS_PER_REQUEST);
        for (List<FieldTypeDTO> batch : nonTextFieldsBatches) {
            final Set<FieldTypeDTO> filteredBatch = aggregationBasedFieldTypeFilterAdapter.filterFieldTypes(new HashSet<>(batch), indexNames, streamIdSingleton);
            fieldTypeDTOsForStream.addAll(filteredBatch);
        }
        return fieldTypeDTOsForStream;
    }

    private Set<FieldTypeDTO> getInitialFieldTypes(final String streamId, final Set<String> indexNames) {
        final Set<String> indexSets = streamService.indexSetIdsByIds(Collections.singletonList(streamId));

        return this.indexFieldTypesService.findForIndexSets(indexSets)
                .stream()
                .filter(fieldTypes -> indexNames.contains(fieldTypes.indexName()))
                .flatMap(fieldTypes -> fieldTypes.fields().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean runsForever() {
        return maintainsStreamBasedFieldLists;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return maintainsStreamBasedFieldLists;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 10;
    }

    @Override
    public int getPeriodSeconds() {
        return 600;
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
