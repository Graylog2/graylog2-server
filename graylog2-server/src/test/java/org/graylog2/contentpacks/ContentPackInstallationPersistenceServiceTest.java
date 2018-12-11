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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class ContentPackInstallationPersistenceServiceTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MongoConnectionRule mongoRule = MongoConnectionRule.build("content_packs");

    private ContentPackInstallationPersistenceService persistenceService;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mongoJackObjectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        persistenceService = new ContentPackInstallationPersistenceService(
                mongoJackObjectMapperProvider,
                mongoRule.getMongoConnection());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loadAll() {
        final Set<ContentPackInstallation> contentPacks = persistenceService.loadAll();
        assertThat(contentPacks).hasSize(4);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findById() {
        final ObjectId objectId = new ObjectId("5b4c935b4b900a0000000001");
        final Optional<ContentPackInstallation> contentPacks = persistenceService.findById(objectId);

        assertThat(contentPacks)
                .isPresent()
                .get()
                .satisfies(contentPackInstallation -> assertThat(contentPackInstallation.id()).isEqualTo(objectId));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByIdWithInvalidId() {
        final Optional<ContentPackInstallation> contentPacks = persistenceService.findById(new ObjectId("0000000000000000deadbeef"));
        assertThat(contentPacks).isEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByContentPackId() {
        final ModelId id = ModelId.of("4e3d7025-881e-6870-da03-cafebabe0001");
        final Set<ContentPackInstallation> contentPacks = persistenceService.findByContentPackId(id);

        assertThat(contentPacks)
                .hasSize(2)
                .allSatisfy(contentPackInstallation -> assertThat(contentPackInstallation.contentPackId()).isEqualTo(id))
                .anySatisfy(contentPackInstallation -> assertThat(contentPackInstallation.id()).isEqualTo(new ObjectId("5b4c935b4b900a0000000001")))
                .anySatisfy(contentPackInstallation -> assertThat(contentPackInstallation.id()).isEqualTo(new ObjectId("5b4c935b4b900a0000000002")));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByContentPackIdWithInvalidId() {
        final Set<ContentPackInstallation> contentPacks = persistenceService.findByContentPackId(ModelId.of("does-not-exist"));

        assertThat(contentPacks).isEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByContentPackIdAndRevision() {
        final ModelId id = ModelId.of("4e3d7025-881e-6870-da03-cafebabe0001");
        final Set<ContentPackInstallation> contentPack = persistenceService.findByContentPackIdAndRevision(id, 1);

        assertThat(contentPack)
                .hasSize(1)
                .anySatisfy(c -> assertThat(c.contentPackId()).isEqualTo(id));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByContentPackIdAndRevisionWithInvalidId() {
        final Set<ContentPackInstallation> contentPack = persistenceService.findByContentPackIdAndRevision(ModelId.of("4e3d7025-881e-6870-da03-cafebabe0001"), 3);

        assertThat(contentPack).isEmpty();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void insert() {
        final ContentPackInstallation contentPackInstallation = ContentPackInstallation.builder()
                .contentPackId(ModelId.of("content-pack-id"))
                .contentPackRevision(1)
                .parameters(ImmutableMap.of())
                .entities(ImmutableSet.of())
                .comment("Comment")
                .createdAt(ZonedDateTime.of(2018, 7, 16, 14, 0, 0, 0, ZoneOffset.UTC).toInstant())
                .createdBy("username")
                .build();

        final ContentPackInstallation savedContentPack = persistenceService.insert(contentPackInstallation);
        assertThat(savedContentPack.id()).isNotNull();
        assertThat(savedContentPack).isEqualToIgnoringGivenFields(contentPackInstallation, "id");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteById() {
        final ObjectId objectId = new ObjectId("5b4c935b4b900a0000000001");
        final int deletedContentPacks = persistenceService.deleteById(objectId);
        final Set<ContentPackInstallation> contentPacks = persistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(1);
        assertThat(contentPacks)
                .hasSize(3)
                .noneSatisfy(contentPack -> assertThat(contentPack.id()).isEqualTo(objectId));
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteByIdWithInvalidId() {
        final int deletedContentPacks = persistenceService.deleteById(new ObjectId("0000000000000000deadbeef"));
        final Set<ContentPackInstallation> contentPacks = persistenceService.loadAll();

        assertThat(deletedContentPacks).isEqualTo(0);
        assertThat(contentPacks).hasSize(4);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void countInstallationOfEntityById() {
        final long countedInstallations1 = persistenceService.countInstallationOfEntityById(ModelId.of("5b1a49eb3d274631fe07c86a"));
        assertThat(countedInstallations1).isEqualTo(2);

        final long countedInstallations2 = persistenceService.countInstallationOfEntityById(ModelId.of("non-exsistant"));
        assertThat(countedInstallations2).isEqualTo(0);

        final long countedInstallations3 = persistenceService.countInstallationOfEntityById(ModelId.of("5b1a49eb3d274631fe07befa"));
        assertThat(countedInstallations3).isEqualTo(1);
    }
}