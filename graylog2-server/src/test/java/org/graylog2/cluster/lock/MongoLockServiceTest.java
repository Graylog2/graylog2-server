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
package org.graylog2.cluster.lock;

import com.mongodb.client.ListIndexesIterable;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.plugin.system.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.graylog2.cluster.lock.Lock.FIELD_UPDATED_AT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
public class MongoLockServiceTest {

    private LockService lockService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        lockService = new MongoLockService(mockNodeId("some-node-id"), mongodb.mongoConnection(), MongoLockService.MIN_LOCK_TTL);
    }

    @Test
    void newLock() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        final Optional<Lock> lock = lockService.lock("test-resource");

        assertThat(lock).hasValueSatisfying(l -> {
            assertThat(l.resourceName()).isEqualTo("test-resource");
            assertThat(l.lockedBy()).isEqualTo("some-node-id");
            assertThat(l.createdAt()).isCloseTo(now, within(10, SECONDS));
            assertThat(l.updatedAt()).isCloseTo(now, within(10, SECONDS));
        });
    }

    @Test
    void reentrantLock() {
        final Lock orig = lockService.lock("test-resource")
                .orElseThrow(() -> new IllegalStateException("Unable to create original lock."));

        final Optional<Lock> lock = lockService.lock("test-resource");

        assertThat(lock).hasValueSatisfying(l -> {
            assertThat(l.resourceName()).isEqualTo(orig.resourceName());
            assertThat(l.lockedBy()).isEqualTo(orig.lockedBy());
            assertThat(l.createdAt()).isEqualTo(orig.createdAt());
            assertThat(l.updatedAt()).isAfter(orig.updatedAt());
        });
    }

    @Test
    void alreadyTaken(MongoDBTestService mongodb) {
        new MongoLockService(mockNodeId("other-node-id"), mongodb.mongoConnection(), MongoLockService.MIN_LOCK_TTL).lock("test-resource")
                .orElseThrow(() -> new IllegalStateException("Unable to create original lock."));

        final Optional<Lock> lock = lockService.lock("test-resource");

        assertThat(lock).isEmpty();
    }

    @Test
    void unlock(MongoDBTestService mongodb) {
        final MongoLockService otherNodesLockService =
                new MongoLockService(mockNodeId("other-node-id"), mongodb.mongoConnection(), MongoLockService.MIN_LOCK_TTL);

        final Lock orig = otherNodesLockService.lock("test-resource")
                .orElseThrow(() -> new IllegalStateException("Unable to create original lock."));
        assertThat(lockService.lock("test-resource")).isEmpty();

        final Optional<Lock> deletedLock = otherNodesLockService.unlock("test-resource");
        assertThat(deletedLock).hasValueSatisfying(l -> {
            assertThat(l.resourceName()).isEqualTo(orig.resourceName());
            assertThat(l.lockedBy()).isEqualTo(orig.lockedBy());
        });

        assertThat(lockService.lock("test-resource")).isNotEmpty();
    }

    @Test
    void unlockNonExistentLock() {
        assertThat(lockService.unlock("test-resource")).isEmpty();
    }

    @Test
    void ensureTTLIndex(MongoDBTestService mongodb) {
        new MongoLockService(mockNodeId("node-id"), mongodb.mongoConnection(), Duration.ofSeconds(72));

        final ListIndexesIterable<Document> indices = mongodb.mongoCollection(MongoLockService.COLLECTION_NAME).listIndexes();
        boolean found = false;
        for (Document doc : indices) {
            final Set<String> keySet = doc.get("key", Document.class).keySet();
            if (keySet.contains(FIELD_UPDATED_AT)) {
                if (Objects.equals(doc.getLong("expireAfterSeconds"), Duration.ofSeconds(72).getSeconds())) {
                    found = true;
                }
            }
        }

        assertThat(found).isTrue();
    }

    private NodeId mockNodeId(String id) {
        NodeId nodeId = mock(NodeId.class);
        when(nodeId.toString()).thenReturn(id);
        return nodeId;
    }
}
