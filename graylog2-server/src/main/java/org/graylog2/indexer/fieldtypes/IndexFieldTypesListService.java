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
import org.graylog2.database.PaginatedList;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.fieldtypes.mapping.FieldTypeMappingsService;
import org.graylog2.indexer.fieldtypes.utils.FieldTypeDTOsMerger;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType;
import org.mongojack.DBQuery;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.graylog2.indexer.fieldtypes.FieldTypeMapper.TYPE_MAP;
import static org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO.FIELD_INDEX_NAME;
import static org.graylog2.indexer.indexset.CustomFieldMappings.REVERSE_TYPES;

public class IndexFieldTypesListService {

    private IndexFieldTypesService indexFieldTypesService;
    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory indexSetFactory;

    private FieldTypeDTOsMerger fieldTypeDTOsMerger;

    @Inject
    public IndexFieldTypesListService(final IndexFieldTypesService indexFieldTypesService,
                                      final IndexSetService indexSetService,
                                      final MongoIndexSet.Factory indexSetFactory,
                                      final FieldTypeDTOsMerger fieldTypeDTOsMerger) {
        this.indexFieldTypesService = indexFieldTypesService;
        this.indexSetService = indexSetService;
        this.indexSetFactory = indexSetFactory;
        this.fieldTypeDTOsMerger = fieldTypeDTOsMerger;
    }

    public PageListResponse<IndexSetFieldType> getIndexSetFieldTypesList(
            final String indexSetId,
            final int page,
            final int perPage,
            final String sort,
            final Sorting.Direction order) {

        final Optional<IndexSetConfig> indexSetConfig = indexSetService.get(indexSetId);
        final Optional<MongoIndexSet> mongoIndexSet = indexSetConfig.map(indexSetFactory::create);

        final CustomFieldMappings customFieldMappings = indexSetConfig.map(IndexSetConfig::customFieldMappings).orElse(new CustomFieldMappings());

        final Set<FieldTypeDTO> deflectorFieldDtos = mongoIndexSet
                .map(MongoIndexSet::getActiveWriteIndex)
                .map(indexName -> indexFieldTypesService.findOneByQuery(DBQuery.is(FIELD_INDEX_NAME, indexName)))
                .map(IndexFieldTypesDTO::fields)
                .orElse(ImmutableSet.of());

        final Set<FieldTypeDTO> previousFieldDtos = mongoIndexSet
                .map(this::getPreviousActiveIndexSet)
                .map(indexName -> indexFieldTypesService.findOneByQuery(DBQuery.is(FIELD_INDEX_NAME, indexName)))
                .map(IndexFieldTypesDTO::fields)
                .orElse(ImmutableSet.of());

        final Collection<FieldTypeDTO> allFields = fieldTypeDTOsMerger.merge(deflectorFieldDtos, previousFieldDtos, customFieldMappings);
        final List<IndexSetFieldType> retrievedPage = allFields
                .stream()
                .map(fieldTypeDTO -> new IndexSetFieldType(
                                fieldTypeDTO.fieldName(),
                                REVERSE_TYPES.get(TYPE_MAP.get(fieldTypeDTO.physicalType())),
                                customFieldMappings.containsCustomMappingForField(fieldTypeDTO.fieldName()),
                                FieldTypeMappingsService.BLACKLISTED_FIELDS.contains(fieldTypeDTO.fieldName())
                        )
                )
                .sorted(IndexSetFieldType.getComparator(sort, order))
                .skip((long) Math.max(0, page - 1) * perPage)
                .limit(perPage)
                .toList();

        final int total = allFields.size();

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

    private String getPreviousActiveIndexSet(final MongoIndexSet indexSet) {
        final String activeWriteIndex = indexSet.getActiveWriteIndex();
        if (activeWriteIndex != null) {
            final Optional<Integer> deflectorNumber = indexSet.extractIndexNumber(activeWriteIndex);
            final String indexPrefix = indexSet.getIndexPrefix();
            return deflectorNumber.map(num -> indexPrefix + MongoIndexSet.SEPARATOR + (num - 1)).orElse(null);
        }

        return null;
    }
}
