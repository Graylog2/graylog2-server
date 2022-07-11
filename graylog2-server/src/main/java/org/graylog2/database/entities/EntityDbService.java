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
package org.graylog2.database.entities;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * DB service to serve as a base class for database services that handle entities with metadata.
 * The operative bit is <DTO extends Entity>, which ensures that the entity that the DB service is being used with
 * extends the appropriate Entity interface.
 */

public abstract class EntityDbService<DTO extends Entity> extends PaginatedDbService<DTO> {

    protected final EntityScopeService entityScopeService;

    public EntityDbService(MongoConnection mongoConnection,
                           MongoJackObjectMapperProvider mapper,
                           Class<DTO> dtoClass, String collectionName,
                           EntityScopeService entityScopeService) {
        super(mongoConnection, mapper, dtoClass, collectionName);
        this.entityScopeService = entityScopeService;
    }

    @Override
    public DTO save(DTO dto) {

        ensureValidScope(dto);
        if (dto.id() != null) {
            ensureMutability(dto);
        }
        final EntityMetadata newMetadata = getUpdatedMetadata(dto);
        return super.save(dto.withMetadata(newMetadata));
    }

    private void ensureValidScope(DTO dto) {
        if (!entityScopeService.hasValidScope(dto)) {
            throw new IllegalArgumentException("Invalid Entity Scope: " + dto.metadata().scope());
        }
    }

    private EntityMetadata getUpdatedMetadata(DTO dirtyDto) {

        Optional<DTO> optCurrent = dirtyDto.id() == null ? Optional.empty() : get(dirtyDto.id());

        final EntityMetadata metadata;
        final long revision;

        if (optCurrent.isPresent()) {
            metadata = optCurrent.get().metadata();
            revision = metadata.rev() + 1;
        } else {
            metadata = dirtyDto.metadata().toBuilder()
                    .createdAt(ZonedDateTime.now(ZoneOffset.UTC))
                    .build();
            revision = EntityMetadata.DEFAULT_REV;
        }

        return metadata.toBuilder()
                .updatedAt(ZonedDateTime.now(ZoneOffset.UTC))
                .rev(revision)
                .build();
    }
    
    private void ensureMutability(DTO dto) {
        if (!entityScopeService.isMutable(dto)) {
            throw new IllegalArgumentException("Immutable entity cannot be modified");
        }
    }

    @Override
    public int delete(String id) {
        ensureMutability(get(id).orElseThrow(() -> new IllegalArgumentException("Entity not found")));

        // TODO: Implement without load + delete
        return super.delete(id);
    }
}
