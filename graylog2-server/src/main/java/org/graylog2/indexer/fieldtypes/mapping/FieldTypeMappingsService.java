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
package org.graylog2.indexer.fieldtypes.mapping;

import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.rest.bulk.model.BulkOperationFailure;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Message.FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS;

public class FieldTypeMappingsService {

    private static final Logger LOG = LoggerFactory.getLogger(FieldTypeMappingsService.class);

    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory mongoIndexSetFactory;
    private final MongoIndexSetService mongoIndexSetService;

    private final IndexFieldTypeProfileService profileService;

    @Inject
    public FieldTypeMappingsService(final IndexSetService indexSetService,
                                    final MongoIndexSet.Factory mongoIndexSetFactory,
                                    final MongoIndexSetService mongoIndexSetService,
                                    final IndexFieldTypeProfileService profileService) {
        this.indexSetService = indexSetService;
        this.mongoIndexSetFactory = mongoIndexSetFactory;
        this.mongoIndexSetService = mongoIndexSetService;
        this.profileService = profileService;
    }

    public void changeFieldType(final CustomFieldMapping customMapping,
                                final Set<String> indexSetsIds,
                                final boolean rotateImmediately) {
        checkFieldTypeCanBeChanged(customMapping.fieldName());
        checkType(customMapping);

        for (String indexSetId : indexSetsIds) {
            try {
                indexSetService.get(indexSetId).ifPresent(indexSetConfig -> {
                    var updatedIndexSetConfig = storeMapping(customMapping, indexSetConfig);
                    if (rotateImmediately) {
                        updatedIndexSetConfig.ifPresent(this::cycleIndexSet);
                    }
                });
            } catch (Exception ex) {
                LOG.error("Failed to update field type in index set : " + indexSetId, ex);
                throw ex;
            }
        }
    }

    public void setProfile(final Set<String> indexSetsIds,
                           final String profileId,
                           final boolean rotateImmediately) {
        checkProfile(profileId);
        for (String indexSetId : indexSetsIds) {
            try {
                indexSetService.get(indexSetId).ifPresent(indexSetConfig -> {
                    var updatedIndexSetConfig = setProfileForIndexSet(profileId, indexSetConfig);
                    if (rotateImmediately) {
                        updatedIndexSetConfig.ifPresent(this::cycleIndexSet);
                    }
                });
            } catch (Exception ex) {
                LOG.error("Failed to update field type in index set : " + indexSetId, ex);
                throw ex;
            }
        }
    }

    public void removeProfileFromIndexSets(final Set<String> indexSetsIds,
                                           final boolean rotateImmediately) {
        for (String indexSetId : indexSetsIds) {
            try {
                indexSetService.get(indexSetId).ifPresent(indexSetConfig -> {
                    var updatedIndexSetConfig = removeProfileFromIndexSet(indexSetConfig);
                    if (rotateImmediately) {
                        updatedIndexSetConfig.ifPresent(this::cycleIndexSet);
                    }
                });
            } catch (Exception ex) {
                LOG.error("Failed to update field type in index set : " + indexSetId, ex);
                throw ex;
            }
        }
    }

    public Map<String, BulkOperationResponse> removeCustomMappingForFields(final List<String> fieldNames,
                                                                           final Set<String> indexSetsIds,
                                                                           final boolean rotateImmediately) {
        Map<String, BulkOperationResponse> result = new HashMap<>();
        for (String indexSetId : indexSetsIds) {
            try {
                indexSetService.get(indexSetId).ifPresentOrElse(
                        indexSetConfig -> result.put(indexSetId, removeMappings(fieldNames, indexSetConfig, rotateImmediately)),
                        () -> result.put(indexSetId, new BulkOperationResponse(List.of("Index set with following ID not present in the database: " + indexSetId)))
                );
            } catch (Exception ex) {
                LOG.error("Failed to remove custom mappings for fields " + fieldNames.toString() + " in index set : " + indexSetId, ex);
                result.put(indexSetId, new BulkOperationResponse(List.of("Exception while removing custom field mappings for index set : " + indexSetId + ": " + ex.getMessage())));
            }
        }
        return result;
    }

    private BulkOperationResponse removeMappings(final List<String> fieldNames,
                                                 final IndexSetConfig indexSetConfig,
                                                 final boolean rotateImmediately) {
        final CustomFieldMappings previousCustomFieldMappings = indexSetConfig.customFieldMappings();
        final Set<String> fieldsWithoutCustomMappings = fieldNames.stream()
                .filter(fieldName -> !previousCustomFieldMappings.containsCustomMappingForField(fieldName))
                .collect(Collectors.toSet());
        final boolean removedSmth = previousCustomFieldMappings.removeIf(customFieldMapping -> fieldNames.stream().anyMatch(fieldName -> customFieldMapping.fieldName().equals(fieldName)));
        final int fieldsRemoved = fieldNames.size() - fieldsWithoutCustomMappings.size();
        final List<BulkOperationFailure> failures = fieldsWithoutCustomMappings.stream()
                .map(f -> new BulkOperationFailure(f, "Field not present in custom mappings"))
                .collect(Collectors.toCollection(ArrayList::new));
        final List<String> errors = new LinkedList<>();
        if (removedSmth) {
            var updatedIndexSetConfig = Optional.of(mongoIndexSetService.save(
                    indexSetConfig.toBuilder()
                            .customFieldMappings(previousCustomFieldMappings)
                            .build()
            ));

            if (rotateImmediately) {
                try {
                    updatedIndexSetConfig.ifPresent(this::cycleIndexSet);
                } catch (Exception ex) {
                    errors.add("Failed to rotate index set after successful custom mapping removal: " + ex.getMessage());
                    LOG.error("Failed to rotate index set after successful custom mapping removal for fields " + fieldNames.toString() + " in index set : " + indexSetConfig.id(), ex);
                }
            }

        }
        return new BulkOperationResponse(fieldsRemoved,
                failures,
                errors);
    }

    private Optional<IndexSetConfig> storeMapping(final CustomFieldMapping customMapping,
                                                  final IndexSetConfig indexSetConfig) {
        final CustomFieldMappings previousCustomFieldMappings = indexSetConfig.customFieldMappings();
        if (previousCustomFieldMappings.contains(customMapping)) {
            return Optional.empty();
        }
        return Optional.of(mongoIndexSetService.save(
                indexSetConfig.toBuilder()
                        .customFieldMappings(previousCustomFieldMappings.mergeWith(customMapping))
                        .build()
        ));
    }

    private Optional<IndexSetConfig> setProfileForIndexSet(final String profileId,
                                                           final IndexSetConfig indexSetConfig) {
        if (Objects.equals(indexSetConfig.fieldTypeProfile(), profileId)) {
            return Optional.empty();
        }
        return Optional.of(mongoIndexSetService.save(
                indexSetConfig.toBuilder()
                        .fieldTypeProfile(profileId)
                        .build()
        ));
    }

    private Optional<IndexSetConfig> removeProfileFromIndexSet(final IndexSetConfig indexSetConfig) {
        if (indexSetConfig.fieldTypeProfile() == null) {
            return Optional.empty();
        }
        return Optional.of(mongoIndexSetService.save(
                indexSetConfig.toBuilder()
                        .fieldTypeProfile(null)
                        .build()
        ));
    }

    private void cycleIndexSet(final IndexSetConfig indexSetConfig) {
        final MongoIndexSet mongoIndexSet = mongoIndexSetFactory.create(indexSetConfig);
        mongoIndexSet.cycle();
    }

    private void checkType(final CustomFieldMapping customMapping) {
        var type = CustomFieldMappings.AVAILABLE_TYPES.get(customMapping.type());
        if (type == null) {
            throw new BadRequestException("Invalid type provided: " + customMapping.type() + " - available types: " + CustomFieldMappings.AVAILABLE_TYPES.keySet());
        }
    }

    private void checkProfile(final String profileId) {
        final Optional<IndexFieldTypeProfile> fieldTypeProfile = profileService.get(profileId);
        if (fieldTypeProfile.isPresent()) {
            fieldTypeProfile.get().customFieldMappings().forEach(mapping -> checkFieldTypeCanBeChanged(mapping.fieldName()));
        } else {
            throw new NotFoundException("No profile with id : " + profileId);
        }
    }

    private void checkFieldTypeCanBeChanged(final String fieldName) {
        if (FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS.contains(fieldName)) {
            throw new BadRequestException("Unable to change field type of " + fieldName + ", not allowed to change type of these fields: " + FIELDS_UNCHANGEABLE_BY_CUSTOM_MAPPINGS);
        }
    }
}
