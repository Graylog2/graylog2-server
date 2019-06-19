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
package org.graylog2.system.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DBProcessingStatusServiceTest {
    private static final String NODE_ID = "abc-123";

    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private NodeId nodeId;

    private DBProcessingStatusService dbService;

    @Before
    public void setUp() throws Exception {
        when(nodeId.toString()).thenReturn(NODE_ID);

        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);

        dbService = new DBProcessingStatusService(mongoRule.getMongoConnection(), nodeId, mapperProvider);
    }

    @Test
    @UsingDataSet(locations = "processing-status.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loadPersisted() {
        assertThat(dbService.all()).hasSize(1);

        assertThat(dbService.all().get(0)).satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("54e3deadbeefdeadbeef0000");
            assertThat(dto.nodeId()).isEqualTo("abc-123");
            assertThat(dto.updatedAt()).isEqualByComparingTo(DateTime.parse("2019-01-01T00:03:00.000Z"));

            assertThat(dto.maxReceiveTimes()).satisfies(maxReceiveTimes -> {
                assertThat(maxReceiveTimes.preJournal()).isEqualByComparingTo(DateTime.parse("2019-01-01T00:03:00.000Z"));
                assertThat(maxReceiveTimes.postProcessing()).isEqualByComparingTo(DateTime.parse("2019-01-01T00:02:00.000Z"));
                assertThat(maxReceiveTimes.postIndexing()).isEqualByComparingTo(DateTime.parse("2019-01-01T00:01:00.000Z"));
            });
        });
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void persistAndUpdate() {
        final InMemoryProcessingStatusRecorder statusRecorder = new InMemoryProcessingStatusRecorder();
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        statusRecorder.updatePreJournalMaxReceiveTime(now);
        statusRecorder.updatePostProcessingMaxReceiveTime(now.minusSeconds(1));
        statusRecorder.updatePostIndexingMaxReceiveTime(now.minusSeconds(2));

        assertThat(dbService.save(statusRecorder, now)).satisfies(dto -> {
            assertThat(dto.id()).isNotBlank();
            assertThat(dto.nodeId()).isEqualTo(NODE_ID);
            assertThat(dto.updatedAt()).isEqualByComparingTo(now);

            assertThat(dto.maxReceiveTimes()).satisfies(maxReceiveTimes -> {
                assertThat(maxReceiveTimes.preJournal()).isEqualByComparingTo(now);
                assertThat(maxReceiveTimes.postProcessing()).isEqualByComparingTo(now.minusSeconds(1));
                assertThat(maxReceiveTimes.postIndexing()).isEqualByComparingTo(now.minusSeconds(2));
            });
        });

        assertThat(dbService.all()).hasSize(1);

        // Advance time and update the status recorder
        final DateTime tomorrow = now.plusDays(1);

        statusRecorder.updatePreJournalMaxReceiveTime(tomorrow);
        statusRecorder.updatePostProcessingMaxReceiveTime(tomorrow.minusSeconds(1));
        statusRecorder.updatePostIndexingMaxReceiveTime(tomorrow.minusSeconds(2));

        // Save the updated recorder
        assertThat(dbService.save(statusRecorder, tomorrow)).satisfies(dto -> {
            assertThat(dto.id()).isNotBlank();
            assertThat(dto.nodeId()).isEqualTo(NODE_ID);
            assertThat(dto.updatedAt()).isEqualByComparingTo(tomorrow);

            assertThat(dto.maxReceiveTimes()).satisfies(maxReceiveTimes -> {
                assertThat(maxReceiveTimes.preJournal()).isEqualByComparingTo(tomorrow);
                assertThat(maxReceiveTimes.postProcessing()).isEqualByComparingTo(tomorrow.minusSeconds(1));
                assertThat(maxReceiveTimes.postIndexing()).isEqualByComparingTo(tomorrow.minusSeconds(2));
            });
        });

        // The save() should be an upsert so we should only have one document
        assertThat(dbService.all()).hasSize(1);
    }
}