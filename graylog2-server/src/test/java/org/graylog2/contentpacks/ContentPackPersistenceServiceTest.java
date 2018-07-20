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
package org.graylog2.contentpacks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackV1;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class ContentPackPersistenceServiceTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("content_packs");

    private ContentPackPersistenceService contentPackPersistenceService;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        contentPackPersistenceService = new ContentPackPersistenceService(
                mongoJackObjectMapperProvider,
                mongoRule.getMongoConnection());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loadAll() {
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(contentPacks)
                .hasSize(5);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loadAllLatest() {
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAllLatest();

        assertThat(contentPacks)
                .hasSize(3)
                .anyMatch(contentPack -> contentPack.id().equals(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000")) && contentPack.revision() == 3);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findAllById() {
        final Set<ContentPack> contentPacks = contentPackPersistenceService.findAllById(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"));

        assertThat(contentPacks)
                .hasSize(3)
                .allMatch(contentPack -> contentPack.id().equals(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000")));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findAllByIdWithInvalidId() {
        final Set<ContentPack> contentPacks = contentPackPersistenceService.findAllById(ModelId.of("does-not-exist"));

        assertThat(contentPacks).isEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByIdAndRevision() {
        final Optional<ContentPack> contentPack = contentPackPersistenceService.findByIdAndRevision(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"), 2);

        assertThat(contentPack)
                .isPresent()
                .get()
                .matches(c -> c.id().equals(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000")));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByIdAndRevisionWithInvalidId() {
        final Optional<ContentPack> contentPack = contentPackPersistenceService.findByIdAndRevision(ModelId.of("does-not-exist"), 2);

        assertThat(contentPack).isEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByIdAndRevisionWithInvalidRevision() {
        final Optional<ContentPack> contentPack = contentPackPersistenceService.findByIdAndRevision(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"), 42);

        assertThat(contentPack).isEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void insert() {
        final ContentPackV1 contentPack = ContentPackV1.builder()
                .id(ModelId.of("id"))
                .revision(1)
                .name("name")
                .description("description")
                .summary("summary")
                .vendor("vendor")
                .url(URI.create("https://www.graylog.org/"))
                .entities(ImmutableSet.of())
                .build();

        final Optional<ContentPack> savedContentPack = contentPackPersistenceService.insert(contentPack);
        assertThat(savedContentPack)
                .isPresent()
                .get()
                .isEqualToIgnoringGivenFields(contentPack, "_id");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void insertDuplicate() {
        final ContentPackV1 contentPack = ContentPackV1.builder()
                .id(ModelId.of("id"))
                .revision(1)
                .name("name")
                .description("description")
                .summary("summary")
                .vendor("vendor")
                .url(URI.create("https://www.graylog.org/"))
                .entities(ImmutableSet.of())
                .build();
        contentPackPersistenceService.insert(contentPack);

        final Optional<ContentPack> savedContentPack2 = contentPackPersistenceService.insert(contentPack);
        assertThat(savedContentPack2)
                .isEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteById() {
        final int deletedContentPacks = contentPackPersistenceService.deleteById(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"));
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(3);
        assertThat(contentPacks)
                .hasSize(2)
                .noneMatch(contentPack -> contentPack.id().equals(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000")));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteByIdWithInvalidId() {
        final int deletedContentPacks = contentPackPersistenceService.deleteById(ModelId.of("does-not-exist"));
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(0);
        assertThat(contentPacks)
                .hasSize(5);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteByIdAndRevision() {
        final int deletedContentPacks = contentPackPersistenceService.deleteByIdAndRevision(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"), 2);
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(1);
        assertThat(contentPacks)
                .hasSize(4)
                .noneMatch(contentPack -> contentPack.id().equals(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000")) && contentPack.revision() == 2);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteByIdAndRevisionWithInvalidId() {
        final int deletedContentPacks = contentPackPersistenceService.deleteByIdAndRevision(ModelId.of("does-not-exist"), 2);
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(0);
        assertThat(contentPacks).hasSize(5);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteByIdAndRevisionWithInvalidRevision() {
        final int deletedContentPacks = contentPackPersistenceService.deleteByIdAndRevision(ModelId.of("dcd74ede-6832-4ef7-9f69-deadbeef0000"), 42);
        final Set<ContentPack> contentPacks = contentPackPersistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(0);
        assertThat(contentPacks).hasSize(5);
    }
}