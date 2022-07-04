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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * DB service to serve as a base class for database services that handle entities with metadata.
 * The operative bit is <DTO extends Entity>, which ensures that the entity that the DB service is being used with
 * extends the appropriate Entity interface.
 */
// TODO: Missing pagination implementations
public abstract class EntityDbService<DTO extends Entity<DTO>> {
    private final JacksonDBCollection<DTO, ObjectId> db;
    private final ObjectMapper objectMapper;
    private final Class<DTO> dtoClass;

    public EntityDbService(MongoConnection mongoConnection,
                           MongoJackObjectMapperProvider mapper,
                           Class<DTO> dtoClass,
                           String collectionName) {
        this.objectMapper = mapper.get();
        this.dtoClass = dtoClass;
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(collectionName),
                dtoClass,
                ObjectId.class,
                mapper.get());
    }

    public Optional<DTO> get(String id) {
        return Optional.ofNullable(db.findOneById(new ObjectId(id)));
    }

    public DTO save(DTO dto) {
        if (dto.id() == null) {
            return saveNew(dto);
        } else {
            return saveUpdated(dto);
        }
    }

    private DTO saveNew(DTO dto) {
        final EntityMetadata newMetadata = EntityMetadata.withNewMetadataForInitialSave(dto.metadata());
        final WriteResult<DTO, ObjectId> result = db.save(dto.withMetadata(newMetadata));

        return result.getSavedObject();
    }

    private DTO saveUpdated(DTO dto) {
        ensureMutability(dto);

        final String id = requireNonNull(dto.id(), "Entity ID cannot be null");

        // TODO: This is not safe! We need to figure out a way to create a single update operation where we $set all
        //       entity fields except the _metadata field and add specific metadata update operations (e.g., $inc for
        //       the revision and date updates for the created/updated at fields)
        //       Using db.convertToDbObject(dto) could work, the returned raw object cannot be mixed with DBUpdate, though.
        //       This should become easier once we updated to the latest mongojack.
        final DTO existingEntity = get(id).orElseThrow(() -> new IllegalArgumentException("Entity not found"));

        final DBQuery.Query query = DBQuery.is("_id", new ObjectId(id))
                .is(EntityMetadata.withMetadataPrefix(EntityMetadata.REV), dto.metadata().rev());
        final DTO updatedEntity = dto.withMetadata(existingEntity.metadata());

        final DTO result = db.findAndModify(query, null, null, false, updatedEntity, true, false);

        if (result == null) {
            throw new IllegalArgumentException("Entity <" + id + "> with revision <" + dto.metadata().rev() + "> not found");
        }

        final DBUpdate.Builder update = new DBUpdate.Builder();

        // Adds something like the following to the builder:
        // {"$inc": {"_metadata.rev": 1}, "$set": {"_metadata.updated_at": {"$date": 1656936934105}}}
        EntityMetadata.applyMetadataUpdate(update);

        return db.findAndModify(query, null, null, false, update, true, false);
    }

    // TODO: This needs to call out to a service that determines the mutability of an entity based on its scope
    private void ensureMutability(DTO dto) {
        if (EntityMetadata.DEFAULT_SCOPE.equals(dto.metadata().scope())) {
            return;
        }

        throw new IllegalArgumentException("Immutable entity cannot be modified");
    }

    public int delete(String id) {
        ensureMutability(get(id).orElseThrow(() -> new IllegalArgumentException("Entity not found")));

        // TODO: Implement without load + delete

        return db.removeById(new ObjectId(requireNonNull(id, "id cannot be null"))).getN();
    }
}
