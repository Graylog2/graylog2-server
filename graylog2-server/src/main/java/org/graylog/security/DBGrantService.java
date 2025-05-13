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
package org.graylog.security;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.google.errorprone.annotations.MustBeClosed;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.database.users.User;

import javax.annotation.Nullable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.or;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class DBGrantService {
    public static final String COLLECTION_NAME = "grants";

    private final MongoCollection<GrantDTO> collection;
    private final MongoUtils<GrantDTO> mongoUtils;

    @Inject
    public DBGrantService(MongoCollections mongoCollections) {
        collection = mongoCollections.collection(COLLECTION_NAME, GrantDTO.class);
        mongoUtils = mongoCollections.utils(collection);

        collection.createIndex(Indexes.ascending(GrantDTO.FIELD_GRANTEE));
        collection.createIndex(Indexes.ascending(GrantDTO.FIELD_TARGET));
        collection.createIndex(Indexes.ascending(
                        GrantDTO.FIELD_GRANTEE,
                        GrantDTO.FIELD_CAPABILITY,
                        GrantDTO.FIELD_TARGET),
                new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending(
                        GrantDTO.FIELD_GRANTEE,
                        GrantDTO.FIELD_TARGET),
                new IndexOptions().unique(true));
        // TODO: Add more indices
    }

    public ImmutableSet<GrantDTO> getForGranteesOrGlobal(Set<GRN> grantees) {
        final var iterable = collection.find(
                or(
                        in(GrantDTO.FIELD_GRANTEE, grantees),
                        eq(GrantDTO.FIELD_GRANTEE, GRNRegistry.GLOBAL_USER_GRN.toString())
                )
        );
        return ImmutableSet.copyOf(iterable);
    }

    public ImmutableSet<GrantDTO> getForGrantee(GRN grantee) {
        final var iterable = collection.find(eq(GrantDTO.FIELD_GRANTEE, grantee));
        return ImmutableSet.copyOf(iterable);
    }

    public ImmutableSet<GrantDTO> getForGranteeWithCapability(GRN grantee, Capability capability) {
        final var iterable = collection.find(
                and(
                        eq(GrantDTO.FIELD_GRANTEE, grantee),
                        eq(GrantDTO.FIELD_CAPABILITY, capability)
                )
        );
        return ImmutableSet.copyOf(iterable);
    }

    public ImmutableSet<GrantDTO> getForGranteesOrGlobalWithCapability(Set<GRN> grantees, Capability capability) {
        final var iterable = collection.find(
                and(
                        or(
                                in(GrantDTO.FIELD_GRANTEE, grantees),
                                eq(GrantDTO.FIELD_GRANTEE, GRNRegistry.GLOBAL_USER_GRN.toString())
                        ),
                        eq(GrantDTO.FIELD_CAPABILITY, capability)
                )
        );
        return ImmutableSet.copyOf(iterable);
    }

    public List<GrantDTO> getForTargetAndGrantee(GRN target, GRN grantee) {
        return getForTargetAndGrantees(target, ImmutableSet.of(grantee));
    }

    public List<GrantDTO> getForTargetAndGrantees(GRN target, Set<GRN> grantees) {
        return collection.find(
                and(
                        eq(GrantDTO.FIELD_TARGET, target),
                        in(GrantDTO.FIELD_GRANTEE, grantees)
                )
        ).into(new ArrayList<>());
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

    public ImmutableList<GrantDTO> getAll() {
        return ImmutableList.copyOf(collection.find());
    }

    public List<GrantDTO> getForTarget(GRN target) {
        return collection.find(eq(GrantDTO.FIELD_TARGET, target.toString())).into(new ArrayList<>());
    }

    public int deleteForGrantee(GRN grantee) {
        return Ints.saturatedCast(
                collection.deleteMany(eq(GrantDTO.FIELD_GRANTEE, grantee.toString())).getDeletedCount()
        );
    }

    public int deleteForTarget(GRN target) {
        return Ints.saturatedCast(
                collection.deleteMany(eq(GrantDTO.FIELD_TARGET, target.toString())).getDeletedCount()
        );
    }

    public List<GrantDTO> getForTargetExcludingGrantee(GRN target, GRN grantee) {
        return collection.find(
                and(
                        eq(GrantDTO.FIELD_TARGET, target.toString()),
                        ne(GrantDTO.FIELD_GRANTEE, grantee.toString())
                )
        ).into(new ArrayList<>());
    }

    public Map<GRN, Set<GRN>> getOwnersForTargets(Collection<GRN> targets) {
        final Bson filter = and(
                in(GrantDTO.FIELD_TARGET, targets),
                eq(GrantDTO.FIELD_CAPABILITY, Capability.OWN)
        );
        try (final var stream = MongoUtils.stream(collection.find(filter))) {
            return stream.collect(Collectors.groupingBy(
                    GrantDTO::target,
                    Collectors.mapping(GrantDTO::grantee, Collectors.toSet())
            ));
        }
    }

    public boolean hasGrantFor(GRN grantee, Capability capability, GRN target) {
        return collection.find(and(
                eq(GrantDTO.FIELD_GRANTEE, grantee),
                eq(GrantDTO.FIELD_CAPABILITY, capability),
                eq(GrantDTO.FIELD_TARGET, target)
        )).first() != null;
    }

    public GrantDTO save(GrantDTO grantDTO) {
        return mongoUtils.save(grantDTO);
    }

    public Optional<GrantDTO> get(String id) {
        return mongoUtils.getById(id);
    }

    @MustBeClosed
    public Stream<GrantDTO> streamAll() {
        return MongoUtils.stream(collection.find());
    }

    public int delete(String id) {
        return mongoUtils.deleteById(id) ? 1 : 0;
    }
}
