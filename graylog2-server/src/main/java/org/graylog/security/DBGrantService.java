/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.security;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.users.User;
import org.mongojack.DBQuery;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class DBGrantService extends PaginatedDbService<GrantDTO> {
    public static final String COLLECTION_NAME = "grants";

    private final GRNRegistry grnRegistry;
    private final ClusterEventBus clusterEventBus;
    private final MongoCollection<Document> dbCollection;

    @Inject
    public DBGrantService(MongoConnection mongoConnection,
                          MongoJackObjectMapperProvider mapper,
                          GRNRegistry grnRegistry,
                          ClusterEventBus clusterEventBus) {
        super(mongoConnection, mapper, GrantDTO.class, COLLECTION_NAME);
        this.grnRegistry = grnRegistry;
        this.clusterEventBus = clusterEventBus;
        this.dbCollection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);

        db.createIndex(new BasicDBObject(GrantDTO.FIELD_GRANTEE, 1));
        db.createIndex(new BasicDBObject(GrantDTO.FIELD_TARGET, 1));
        db.createIndex(
                new BasicDBObject(GrantDTO.FIELD_GRANTEE, 1)
                        .append(GrantDTO.FIELD_CAPABILITY, 1)
                        .append(GrantDTO.FIELD_TARGET, 1),
                new BasicDBObject("unique", true));
        db.createIndex(
                new BasicDBObject(GrantDTO.FIELD_GRANTEE, 1)
                        .append(GrantDTO.FIELD_TARGET, 1),
                new BasicDBObject("unique", true));
        // TODO: Add more indices

        // TODO: Inline migration for development. Must be removed before shipping 4.0 GA!
        final MongoCollection<Document> collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
        collection.updateMany(
                Filters.eq(GrantDTO.FIELD_CAPABILITY, "grn::::capability:54e3deadbeefdeadbeef0000"),
                Updates.set(GrantDTO.FIELD_CAPABILITY, Capability.VIEW.toId())
        );
        collection.updateMany(
                Filters.eq(GrantDTO.FIELD_CAPABILITY, "grn::::capability:54e3deadbeefdeadbeef0001"),
                Updates.set(GrantDTO.FIELD_CAPABILITY, Capability.MANAGE.toId())
        );
        collection.updateMany(
                Filters.eq(GrantDTO.FIELD_CAPABILITY, "grn::::capability:54e3deadbeefdeadbeef0002"),
                Updates.set(GrantDTO.FIELD_CAPABILITY, Capability.OWN.toId())
        );
    }

    @Override
    public GrantDTO save(GrantDTO grantDTO) {
        final GrantDTO savedGrantDTO = super.save(grantDTO);
        clusterEventBus.post(GrantChangedEvent.create(savedGrantDTO.id()));
        return savedGrantDTO;
    }

    @Override
    public int delete(String id) {
        final int delete = super.delete(id);
        clusterEventBus.post(GrantChangedEvent.create(id));
        return delete;
    }

    public ImmutableSet<GrantDTO> getForGranteesOrGlobal(Set<GRN> grantees) {
        return streamQuery(DBQuery.or(
                DBQuery.in(GrantDTO.FIELD_GRANTEE, grantees),
                DBQuery.is(GrantDTO.FIELD_GRANTEE, GRNRegistry.GLOBAL_USER_GRN.toString())
        )).collect(ImmutableSet.toImmutableSet());
    }

    public ImmutableSet<GrantDTO> getForGrantee(GRN grantee) {
        return streamQuery(DBQuery.is(GrantDTO.FIELD_GRANTEE, grantee))
                .collect(ImmutableSet.toImmutableSet());
    }

    public ImmutableSet<GrantDTO> getForGranteeWithCapability(GRN grantee, Capability capability) {
        return streamQuery(DBQuery.and(
                DBQuery.is(GrantDTO.FIELD_GRANTEE, grantee),
                DBQuery.is(GrantDTO.FIELD_CAPABILITY, capability)
        )).collect(ImmutableSet.toImmutableSet());
    }

    public List<GrantDTO> getForTargetAndGrantee(GRN target, GRN grantee) {
        return getForTargetAndGrantees(target, ImmutableSet.of(grantee));
    }

    public List<GrantDTO> getForTargetAndGrantees(GRN target, Set<GRN> grantees) {
        return db.find(DBQuery.and(
                DBQuery.is(GrantDTO.FIELD_TARGET, target),
                DBQuery.in(GrantDTO.FIELD_GRANTEE, grantees))).toArray();
    }

    public GrantDTO create(GrantDTO grantDTO, @Nullable User currentUser) {
        return create(grantDTO, requireNonNull(currentUser, "currentUser cannot be null").getName());
    }

    public GrantDTO create(GrantDTO grantDTO, String creatorUsername) {
        checkArgument(isNotBlank(creatorUsername), "creatorUsername cannot be null or empty");
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        return save(grantDTO.toBuilder()
                .createdBy(creatorUsername)
                .createdAt(now)
                .updatedBy(creatorUsername)
                .updatedAt(now)
                .build());
    }

    public GrantDTO create(GRN grantee, Capability capability, GRN target, String creatorUsername) {
        checkArgument(grantee != null, "grantee cannot be null");
        checkArgument(capability != null, "capability cannot be null");
        checkArgument(target != null, "target cannot be null");

        return create(GrantDTO.of(grantee, capability, target), creatorUsername);
    }

    /**
     * Ensure that a grant with the requested or a higher capability exists.
     *
     * @return the created, updated or existing grant
     */
    public GrantDTO ensure(GRN grantee, Capability capability, GRN target, String creatorUsername) {
        final List<GrantDTO> existingGrants = getForTargetAndGrantee(target, grantee);
        if (existingGrants.isEmpty()) {
            return create(grantee, capability, target, creatorUsername);
        }
        // This should never happen
        Preconditions.checkState(existingGrants.size() == 1);

        final GrantDTO grantDTO = existingGrants.get(0);
        // Only upgrade capabilities: VIEW < MANAGE < OWNER
        if (capability.priority() > grantDTO.capability().priority()) {
            final GrantDTO grantUpdate = grantDTO.toBuilder().capability(capability).build();
            return save(grantUpdate);
        }
        return grantDTO;
    }

    public GrantDTO update(GrantDTO updatedGrant, @Nullable User currentUser) {
        final GrantDTO existingGrant = get(updatedGrant.id())
                .orElseThrow(() -> new IllegalArgumentException("Couldn't find grant with ID " + updatedGrant.id()));

        return save(existingGrant.toBuilder()
                .grantee(updatedGrant.grantee())
                .capability(updatedGrant.capability())
                .target(updatedGrant.target())
                .updatedBy(requireNonNull(currentUser, "currentUser cannot be null").getName())
                .updatedAt(ZonedDateTime.now(ZoneOffset.UTC))
                .build());
    }

    public ImmutableSet<GrantDTO> getAll() {
        try (final Stream<GrantDTO> stream = streamAll()) {
            return stream.collect(ImmutableSet.toImmutableSet());
        }
    }

    public List<GrantDTO> getForTarget(GRN target) {
        return db.find(DBQuery.is(GrantDTO.FIELD_TARGET, target.toString())).toArray();
    }

    public long deleteForTarget(GRN target) {
        final Bson filter = Filters.eq(GrantDTO.FIELD_TARGET, target.toString());
        final Set<String> deletedIds = StreamSupport.stream(dbCollection.find(filter).projection(Projections.include()).spliterator(), false)
                .map(doc -> {
                    final Object o = doc.get("_id");
                    if (o instanceof ObjectId) {
                        return ((ObjectId) o).toHexString();
                    }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toSet());
        final DeleteResult deleteResult = dbCollection.deleteMany(filter);

        final long deletedCount = deleteResult.getDeletedCount();
        if (deletedCount > 0) {
            clusterEventBus.post(GrantChangedEvent.create(deletedIds));
        }
        return deletedCount;
    }

    public List<GrantDTO> getForTargetExcludingGrantee(GRN target, GRN grantee) {
        return db.find(DBQuery.and(
                DBQuery.is(GrantDTO.FIELD_TARGET, target.toString()),
                DBQuery.notEquals(GrantDTO.FIELD_GRANTEE, grantee.toString())
        )).toArray();
    }

    public Map<GRN, Set<GRN>> getOwnersForTargets(Collection<GRN> targets) {
        return db.find(DBQuery.and(
                DBQuery.in(GrantDTO.FIELD_TARGET, targets),
                DBQuery.is(GrantDTO.FIELD_CAPABILITY, Capability.OWN)
        )).toArray()
                .stream()
                .collect(Collectors.groupingBy(
                        GrantDTO::target,
                        Collectors.mapping(GrantDTO::grantee, Collectors.toSet())
                ));
    }

    public boolean hasGrantFor(GRN grantee, Capability capability, GRN target) {
        return db.findOne(DBQuery.and(
                DBQuery.is(GrantDTO.FIELD_GRANTEE, grantee),
                DBQuery.is(GrantDTO.FIELD_CAPABILITY, capability),
                DBQuery.is(GrantDTO.FIELD_TARGET, target)
        )) != null;
    }
}
