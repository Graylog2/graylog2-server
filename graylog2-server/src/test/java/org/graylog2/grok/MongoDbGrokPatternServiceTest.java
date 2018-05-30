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
package org.graylog2.grok;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MongoDbGrokPatternServiceTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("grok-test");

    private MongoDbGrokPatternService service;
    private MongoCollection<Document> collection;
    private ClusterEventBus clusterEventBus;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        service = new MongoDbGrokPatternService(
                mongoRule.getMongoConnection(),
                mongoJackObjectMapperProvider,
                clusterEventBus);
        collection = mongoRule
                .getMongoConnection()
                .getMongoDatabase()
                .getCollection(MongoDbGrokPatternService.COLLECTION_NAME);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void load() throws NotFoundException {
        final GrokPattern grokPattern = service.load("56250da2d400000000000001");
        assertThat(grokPattern.name()).isEqualTo("Test1");
        assertThat(grokPattern.pattern()).isEqualTo("[a-z]+");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void loadNonExistentGrokPatternThrowsNotFoundException() {
        assertThatThrownBy(() -> service.load("cafebabe00000000deadbeef"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void bulkLoad() {
        final List<String> idList = ImmutableList.of(
                "56250da2d400000000000001",
                "56250da2d400000000000002",
                "56250da2d4000000deadbeef");

        final Set<GrokPattern> grokPatterns = service.bulkLoad(idList);
        assertThat(grokPatterns)
                .hasSize(2)
                .contains(
                        GrokPattern.create("56250da2d400000000000001", "Test1", "[a-z]+", null),
                        GrokPattern.create("56250da2d400000000000002", "Test2", "[a-z]+", null));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void bulkLoadReturnsEmptySetIfGrokPatternsNotFound() {
        final List<String> idList = ImmutableList.of("56250da2d4000000deadbeef");

        final Set<GrokPattern> grokPatterns = service.bulkLoad(idList);
        assertThat(grokPatterns).isEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loadAll() {
        final Set<GrokPattern> grokPatterns = service.loadAll();
        assertThat(grokPatterns)
                .hasSize(3)
                .contains(
                        GrokPattern.create("56250da2d400000000000001", "Test1", "[a-z]+", null),
                        GrokPattern.create("56250da2d400000000000002", "Test2", "[a-z]+", null),
                        GrokPattern.create("56250da2d400000000000003", "Test3", "%{Test1}-%{Test2}", "56250da2deadbeefcafebabe"));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteAll() {
        assertThat(collection.count()).isEqualTo(3);

        final int deletedRecords = service.deleteAll();
        assertThat(deletedRecords).isEqualTo(3);
        assertThat(collection.count()).isEqualTo(0);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void delete() {
        assertThat(collection.count()).isEqualTo(3);

        final int deletedRecords = service.delete("56250da2d400000000000001");
        assertThat(deletedRecords).isEqualTo(1);
        assertThat(collection.count()).isEqualTo(2);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteNonExistentGrokPattern() {
        assertThat(collection.count()).isEqualTo(3);

        final int deletedRecords = service.delete("56250da2d4000000deadbeef");
        assertThat(deletedRecords).isEqualTo(0);
        assertThat(collection.count()).isEqualTo(3);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void saveAllWithoutReplace() throws ValidationException {
        assertThat(collection.count()).isEqualTo(3);

        final List<GrokPattern> grokPatterns = ImmutableList.of(
                GrokPattern.create("Test", "Pattern"),
                GrokPattern.create("56250da2d400000000000001", "Test", "Pattern", null)
        );
        final List<GrokPattern> savedGrokPatterns = service.saveAll(grokPatterns, false);
        assertThat(savedGrokPatterns).hasSize(2);
        assertThat(collection.count()).isEqualTo(4);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void saveAllWithReplace() throws ValidationException {
        assertThat(collection.count()).isEqualTo(3);

        final List<GrokPattern> grokPatterns = ImmutableList.of(GrokPattern.create("Test", "Pattern"));
        final List<GrokPattern> savedGrokPatterns = service.saveAll(grokPatterns, true);
        assertThat(savedGrokPatterns).hasSize(1);
        assertThat(collection.count()).isEqualTo(1);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void saveAllWithInvalidGrokPattern() {
        final List<GrokPattern> grokPatterns = ImmutableList.of(
                GrokPattern.create("Test", "Pattern"),
                GrokPattern.create("Test", "")
        );
        assertThatThrownBy(() -> service.saveAll(grokPatterns, true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void saveValidGrokPattern() throws ValidationException, NotFoundException {
        assertThat(collection.count()).isEqualTo(0);

        final GrokPattern savedGrokPattern = service.save(GrokPattern.create("Test", "Pattern"));
        assertThat(collection.count()).isEqualTo(1);
        final GrokPattern grokPattern = service.load(savedGrokPattern.id());
        assertThat(grokPattern).isEqualTo(savedGrokPattern);
    }

    @Test
    public void saveInvalidGrokPattern() {
        assertThat(collection.count()).isEqualTo(0);

        assertThatThrownBy(() -> service.save(GrokPattern.create("Test", "%{")))
                .isInstanceOf(ValidationException.class);
        assertThat(collection.count()).isEqualTo(0);

        assertThatThrownBy(() -> service.save(GrokPattern.create("", "[a-z]+")))
                .isInstanceOf(ValidationException.class);
        assertThat(collection.count()).isEqualTo(0);

        assertThatThrownBy(() -> service.save(GrokPattern.create("Test", "")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(collection.count()).isEqualTo(0);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void validateValidGrokPattern() {
        assertThat(service.validate(GrokPattern.create("Test", "%{Test1}"))).isTrue();
    }

    @Test
    @Ignore("Disabled until MongoDbGrokPatternService#validate() has been fixed")
    public void validateInvalidGrokPattern() {
        assertThat(service.validate(GrokPattern.create("Test", "%{"))).isFalse();
        assertThat(service.validate(GrokPattern.create("Test", ""))).isFalse();
        assertThat(service.validate(GrokPattern.create("", "[a-z]+"))).isFalse();
    }
}