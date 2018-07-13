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
package org.graylog2.migrations;

import com.github.fakemongo.Fongo;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.MoreExecutors;
import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPatternsDeletedEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class V2018070614390000_EnforceUniqueGrokPatternsTest {
    private MongoCollection<Document> collection;
    private V2018070614390000_EnforceUniqueGrokPatterns migration;
    private ClusterEventBus clusterEventBus;
    private TestSubscriber subscriber;

    @Before
    public void setUp() {
        final Fongo fongo = new Fongo("migration-test");
        collection = fongo.getDatabase("migration-test").getCollection("grok_patterns");
        subscriber = new TestSubscriber();
        clusterEventBus = new ClusterEventBus(MoreExecutors.newDirectExecutorService());
        clusterEventBus.registerClusterEventSubscriber(subscriber);

        migration = new V2018070614390000_EnforceUniqueGrokPatterns(collection, clusterEventBus);
    }

    @Test
    public void upgradeAbortsIfIndexExists() {
        final IndexOptions indexOptions = new IndexOptions()
                .name("idx_name_asc_unique")
                .unique(true);
        collection.createIndex(Indexes.ascending("name"), indexOptions);

        migration.upgrade();

        assertThat(migration.isIndexCreated()).isFalse();
        assertThat(subscriber.events).isEmpty();
    }

    @Test
    public void upgradeRunsIfIndexDoesNotExist() {
        migration.upgrade();

        assertThat(migration.isIndexCreated()).isTrue();
        assertThat(subscriber.events).isEmpty();
    }

    @Test
    public void upgradeRemovesDuplicateGrokPatterns() {
        collection.insertMany(
                Arrays.asList(
                        grokPattern("FOO", "[a-z]+"),
                        grokPattern("BAR", "%{FOO}[0-9]+"),
                        grokPattern("BAR", "[0-9]+")));
        migration.upgrade();

        assertThat(migration.isIndexCreated()).isTrue();
        assertThat(collection.find())
                .anySatisfy(document -> assertThat(document)
                        .containsEntry("name", "FOO")
                        .containsEntry("pattern", "[a-z]+"))
                .anySatisfy(document -> assertThat(document)
                        .containsEntry("name", "BAR")
                        .containsEntry("pattern", "%{FOO}[0-9]+"))
                .noneSatisfy(document -> assertThat(document)
                        .containsEntry("name", "BAR")
                        .containsEntry("pattern", "[0-9]+"));

        assertThat(subscriber.events)
                .containsOnly(GrokPatternsDeletedEvent.create(Collections.singleton("BAR")));
    }

    @Test
    public void insertingDuplicateGrokPatternsIsNotPossibleAfterUpgrade() {
        collection.insertOne(grokPattern("FOO", "[a-z]+"));

        migration.upgrade();

        assertThatThrownBy(() -> collection.insertOne(grokPattern("FOO", "[a-z]+")))
                .isInstanceOf(DuplicateKeyException.class)
                .hasMessageContaining("E11000 duplicate key error index: migration-test.grok_patterns.idx_name_asc_unique");
    }

    private Document grokPattern(String name, String pattern) {
        return new Document()
                .append("_id", new ObjectId())
                .append("name", name)
                .append("pattern", pattern);
    }

    private static class TestSubscriber {
        public final List<GrokPatternsDeletedEvent> events = new CopyOnWriteArrayList<>();

        @Subscribe
        public void handleGrokPatternsChangedEvent(GrokPatternsDeletedEvent event) {
            events.add(event);
        }
    }
}