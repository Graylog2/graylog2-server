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
package org.graylog2.indexer.fieldtypes;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import jakarta.inject.Inject;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.inmemory.InMemoryFilterExpressionParser;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.fieldtypes.utils.FieldTypeDTOsMerger;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IndexFieldTypesListService {

    private final IndexFieldTypesService indexFieldTypesService;
    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory indexSetFactory;
    private final IndexFieldTypeProfileService profileService;

    private final InMemoryFilterExpressionParser inMemoryFilterExpressionParser;

    private final FieldTypeDTOsMerger fieldTypeDTOsMerger;

    @Inject
    public IndexFieldTypesListService(final IndexFieldTypesService indexFieldTypesService,
                                      final IndexSetService indexSetService,
                                      final MongoIndexSet.Factory indexSetFactory,
                                      final FieldTypeDTOsMerger fieldTypeDTOsMerger,
                                      final InMemoryFilterExpressionParser inMemoryFilterExpressionParser,
                                      final IndexFieldTypeProfileService profileService) {
        this.indexFieldTypesService = indexFieldTypesService;
        this.indexSetService = indexSetService;
        this.indexSetFactory = indexSetFactory;
        this.fieldTypeDTOsMerger = fieldTypeDTOsMerger;
        this.inMemoryFilterExpressionParser = inMemoryFilterExpressionParser;
        this.profileService = profileService;
    }

    public PageListResponse<IndexSetFieldType> getIndexSetFieldTypesListPage(
            final String indexSetId,
            final String fieldNameQuery,
            final List<String> filters,
            final int page,
            final int perPage,
            final String sort,
            final Sorting.Direction order) {

        final List<IndexSetFieldType> filteredFields = getFilteredList(indexSetId, fieldNameQuery, filters, sort, order);
        final int total = filteredFields.size();

        final List<IndexSetFieldType> retrievedPage = filteredFields.stream()
                .skip((long) Math.max(0, page - 1) * perPage)
                .limit(perPage)
                .toList();

        return PageListResponse.create("",
                PaginatedList.PaginationInfo.create(
                        total,
                        retrievedPage.size(),
                        page,
                        perPage),
                total,
                sort,
                order.toString().toLowerCase(Locale.ROOT),
                retrievedPage,
                IndexSetFieldType.ATTRIBUTES,
                IndexSetFieldType.ENTITY_DEFAULTS);

    }

    public List<IndexSetFieldType> getIndexSetFieldTypesList(
            final String indexSetId,
            final String fieldNameQuery,
            final List<String> filters,
            final String sort,
            final Sorting.Direction order) {

        return getFilteredList(indexSetId, fieldNameQuery, filters, sort, order);
    }

    public Map<String, List<IndexSetFieldType>> getIndexSetFieldTypesList(final Set<String> indexSetId, final Collection<String> fieldNameQuery) {
        return getFilteredList(indexSetId, fieldNameQuery);
    }

    @NotNull
    private List<IndexSetFieldType> getFilteredList(String indexSetId, String fieldNameQuery, List<String> filters, String sort, Sorting.Direction order) {
        final Optional<IndexSetConfig> indexSetConfig = indexSetService.get(indexSetId);
        final Optional<IndexSet> mongoIndexSet = indexSetConfig.map(indexSetFactory::create);

        final CustomFieldMappings customFieldMappings = indexSetConfig.map(IndexSetConfig::customFieldMappings).orElse(new CustomFieldMappings());
        final Optional<IndexFieldTypeProfile> fieldTypeProfile = indexSetConfig.map(IndexSetConfig::fieldTypeProfile).flatMap(profileService::get);

        final Set<FieldTypeDTO> deflectorFieldDtos = mongoIndexSet
                .map(IndexSet::getActiveWriteIndex)
                .map(indexFieldTypesService::findOneByIndexName)
                .map(IndexFieldTypesDTO::fields)
                .orElse(ImmutableSet.of());

        final Set<FieldTypeDTO> previousFieldDtos = mongoIndexSet
                .map(this::getPreviousActiveIndexSet)
                .map(indexFieldTypesService::findOneByIndexName)
                .map(IndexFieldTypesDTO::fields)
                .orElse(ImmutableSet.of());

        final Collection<IndexSetFieldType> allFields = fieldTypeDTOsMerger.merge(deflectorFieldDtos,
                previousFieldDtos,
                customFieldMappings,
                fieldTypeProfile.orElse(null));
        return allFields
                .stream()
                .filter(indexSetFieldType -> indexSetFieldType.fieldName().contains(fieldNameQuery))
                .filter(indexSetFieldType -> inMemoryFilterExpressionParser.parse(filters, IndexSetFieldType.ATTRIBUTES).test(indexSetFieldType))
                .sorted(IndexSetFieldType.getComparator(sort, order))
                .toList();
    }

    @NotNull
    private Map<String, List<IndexSetFieldType>> getFilteredList(Set<String> indexSetIds, Collection<String> fieldNames) {
        final var indexSetConfigs = indexSetService.findByIds(indexSetIds);
        final var mongoIndexSets = indexSetConfigs.stream().collect(Collectors.toMap(IndexSetConfig::id, indexSetFactory::create));
        final var activeWriteIndices = mongoIndexSets.values().stream().map(MongoIndexSet::getActiveWriteIndex).collect(Collectors.toSet());
        final var previousActiveIndexSets = mongoIndexSets.values().stream().map(this::getPreviousActiveIndexSet).collect(Collectors.toSet());

        final var indexFieldTypes = indexFieldTypesService.findByIndexNames(Sets.union(activeWriteIndices, previousActiveIndexSets))
                .stream()
                .collect(Collectors.toMap(IndexFieldTypesDTO::indexName, Function.identity()));

        return indexSetConfigs.stream()
                .collect(Collectors.toMap(IndexSetConfig::id,
                        indexSetConfig -> {
                            final var mongoIndexSet = indexSetFactory.create(indexSetConfig);
                            final var customFieldMappings = indexSetConfig.customFieldMappings();
                            final var fieldTypeProfile = Optional.ofNullable(indexSetConfig.fieldTypeProfile())
                                    .flatMap(profileService::get);

                            final Set<FieldTypeDTO> deflectorFieldDtos = Optional.ofNullable(indexFieldTypes.get(mongoIndexSet.getActiveWriteIndex()))
                                    .map(IndexFieldTypesDTO::fields)
                                    .orElse(ImmutableSet.of());

                            final Set<FieldTypeDTO> previousFieldDtos = Optional.ofNullable(indexFieldTypes.get(this.getPreviousActiveIndexSet(mongoIndexSet)))
                                    .map(IndexFieldTypesDTO::fields)
                                    .orElse(ImmutableSet.of());

                            final Collection<IndexSetFieldType> allFields = fieldTypeDTOsMerger.merge(deflectorFieldDtos,
                                    previousFieldDtos,
                                    customFieldMappings,
                                    fieldTypeProfile.orElse(null));
                            return allFields
                                    .stream()
                                    .filter(indexSetFieldType -> fieldNames.contains(indexSetFieldType.fieldName()))
                                    .toList();
                        }));
    }

    private String getPreviousActiveIndexSet(final IndexSet indexSet) {
        return indexSet.getNthIndexBeforeActiveIndexSet(1);
    }
}
