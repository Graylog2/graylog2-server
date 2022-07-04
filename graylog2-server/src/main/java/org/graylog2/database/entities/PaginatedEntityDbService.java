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

import java.util.Optional;

/**
 * DB service to serve as a metadata processing layer above the PaginatedDbService<DTO>.
 * The operative bit is <DTO extends Entity>, which ensures that the entity that the DB service is being used with
 * extends the appropriate Entity interface.
 */
public abstract class PaginatedEntityDbService<DTO extends Entity> extends PaginatedDbService<DTO> {
    public PaginatedEntityDbService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper,
                                    Class<DTO> dtoClass, String collectionName) {
        super(mongoConnection, mapper, dtoClass, collectionName);
    }

    @Override
    public Optional<DTO> get(String id) {
        final Optional<DTO> dto = super.get(id);
        // Perform scope check. Use AOP inspection and existing permissions system.
        // Explicit check performed here as an example.
        if (dto.isPresent()) {
            if (!dto.get().metadata().scopes().contains("arbitrary-permitted-scope")) {
                throw new IllegalStateException("Entity not found.");
            }
        }
        return dto;
    }

    @Override
    public DTO save(DTO dto) {
        // Perform scope check. Probably should get entity from DB to check scope before saving.
        return super.save(dto);
    }

    @Override
    public int delete(String id) {
        // Perform scope check. Probably should get entity from DB to check scope before deleting.
        return super.delete(id);
    }
}
