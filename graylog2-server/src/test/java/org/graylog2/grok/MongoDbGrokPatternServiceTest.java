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

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MongoDbGrokPatternServiceTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private MongoCollection<Document> collection;
    private MongoDbGrokPatternService grokPatternService;

    @Before
    public void setUp() throws Exception {
        final MongoConnection mongoConnection = mongoRule.getMongoConnection();
        collection = mongoConnection.getMongoDatabase().getCollection(MongoDbGrokPatternService.COLLECTION_NAME);

        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
        grokPatternService = new MongoDbGrokPatternService(mongoConnection, mapperProvider);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void saveSucceedsWithValidGrokPattern() throws ValidationException {
        grokPatternService.save(GrokPattern.create("NUMBER", "[0-9]+"));

        assertThat(collection.count()).isEqualTo(1L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void saveFailsWithDuplicateGrokPattern() throws ValidationException {
        grokPatternService.save(GrokPattern.create("NUMBER", "[0-9]+"));

        assertThatThrownBy(() -> grokPatternService.save(GrokPattern.create("NUMBER", "[0-9]+")))
                .isInstanceOf(ValidationException.class)
                .hasMessageStartingWith("Grok pattern NUMBER already exists");
        assertThat(collection.count()).isEqualTo(1L);
    }


    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void saveAllSucceedsWithValidGrokPatterns() throws ValidationException {
        final List<GrokPattern> grokPatterns = Arrays.asList(
                GrokPattern.create("NUMBER", "[0-9]+"),
                GrokPattern.create("INT", "[+-]?%{NUMBER}"));
        grokPatternService.saveAll(grokPatterns, false);

        assertThat(collection.count()).isEqualTo(2L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void saveAllSucceedsWithDuplicateGrokPatternWithReplaceAll() throws ValidationException {
        final List<GrokPattern> grokPatterns = Arrays.asList(
                GrokPattern.create("NUMBER", "[0-9]+"),
                GrokPattern.create("INT", "[+-]?%{NUMBER}"));
        grokPatternService.save(GrokPattern.create("NUMBER", "[0-9]+"));

        grokPatternService.saveAll(grokPatterns, true);

        assertThat(collection.count()).isEqualTo(2L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void saveAllFailsWithDuplicateGrokPatternWithoutReplaceAll() throws ValidationException {
        final List<GrokPattern> grokPatterns = Arrays.asList(
                GrokPattern.create("NUMBER", "[0-9]+"),
                GrokPattern.create("INT", "[+-]?%{NUMBER}"));
        grokPatternService.save(GrokPattern.create("NUMBER", "[0-9]+"));

        assertThatThrownBy(() -> grokPatternService.saveAll(grokPatterns, false))
                .isInstanceOf(ValidationException.class)
                .hasMessageStartingWith("Grok pattern NUMBER already exists");
        assertThat(collection.count()).isEqualTo(1L);
    }
}