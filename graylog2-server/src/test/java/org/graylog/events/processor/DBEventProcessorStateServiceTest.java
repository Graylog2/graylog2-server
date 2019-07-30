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
package org.graylog.events.processor;

import com.google.common.collect.ImmutableSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DBEventProcessorStateServiceTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
    private DBEventProcessorStateService stateService = new DBEventProcessorStateService(mongoRule.getMongoConnection(), objectMapperProvider);

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void persistence() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime min = now.minusHours(1);
        final DateTime max = now;

        final EventProcessorStateDto stateDto = EventProcessorStateDto.builder()
                .eventDefinitionId("abc123")
                .minProcessedTimestamp(min)
                .maxProcessedTimestamp(max)
                .build();

        assertThat(stateService.setState(stateDto)).isPresent().get().satisfies(dto -> {
            assertThat(dto.id()).isNotBlank();
            assertThat(dto.eventDefinitionId()).isEqualTo("abc123");
            assertThat(dto.minProcessedTimestamp()).isEqualTo(min);
            assertThat(dto.maxProcessedTimestamp()).isEqualTo(max);
        });

        assertThatThrownBy(() -> stateService.setState("", min, max))
                .hasMessageContaining("eventDefinitionId")
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stateService.setState(null, min, max))
                .hasMessageContaining("eventDefinitionId")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> stateService.setState("a", null, max))
                .hasMessageContaining("minProcessedTimestamp")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> stateService.setState("a", min, null))
                .hasMessageContaining("maxProcessedTimestamp")
                .isInstanceOf(IllegalArgumentException.class);

        // A max timestamp that is older than the min timestamp is an error! (e.g. mixing up arguments)
        assertThatThrownBy(() -> stateService.setState("a", max, min))
                .hasMessageContaining("minProcessedTimestamp")
                .hasMessageContaining("maxProcessedTimestamp")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @UsingDataSet(locations = "event-processor-state.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loading() {
        final Optional<EventProcessorStateDto> stateDto = stateService.findByEventDefinitionId("54e3deadbeefdeadbeefaff3");

        assertThat(stateDto).isPresent().get().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo("54e3deadbeefdeadbeefaffe");
            assertThat(dto.eventDefinitionId()).isEqualTo("54e3deadbeefdeadbeefaff3");
            assertThat(dto.minProcessedTimestamp()).isEqualTo(DateTime.parse("2019-01-01T00:00:00.000Z"));
            assertThat(dto.maxProcessedTimestamp()).isEqualTo(DateTime.parse("2019-01-01T01:00:00.000Z"));
        });
    }

    @Test
    @UsingDataSet(locations = "event-processor-state.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByEventProcessorId() {
        assertThat(stateService.findByEventDefinitionId("54e3deadbeefdeadbeefaff3")).isPresent();

        assertThat(stateService.findByEventDefinitionId("nope")).isNotPresent();

        assertThatThrownBy(() -> stateService.findByEventDefinitionId(null))
                .hasMessageContaining("eventDefinitionId")
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> stateService.findByEventDefinitionId(""))
                .hasMessageContaining("eventDefinitionId")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @UsingDataSet(locations = "event-processor-state.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findByEventProcessorsAndMaxTimestamp() {
        assertThat(stateService.findByEventDefinitionId("54e3deadbeefdeadbeefaff3")).isPresent().get().satisfies(dto -> {
            final DateTime maxTs = dto.maxProcessedTimestamp();
            final String id = dto.eventDefinitionId();

            assertThat(stateService.findByEventDefinitionsAndMaxTimestamp(ImmutableSet.of(id), maxTs))
                    .hasSize(1);
            assertThat(stateService.findByEventDefinitionsAndMaxTimestamp(ImmutableSet.of(id), maxTs.minusHours(1)))
                    .hasSize(1);
            assertThat(stateService.findByEventDefinitionsAndMaxTimestamp(ImmutableSet.of(id), maxTs.plusHours(1)))
                    .hasSize(0);

            assertThatThrownBy(() -> stateService.findByEventDefinitionsAndMaxTimestamp(ImmutableSet.of(), maxTs))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> stateService.findByEventDefinitionsAndMaxTimestamp(null, maxTs))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> stateService.findByEventDefinitionsAndMaxTimestamp(ImmutableSet.of(id), null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(stateService.findByEventDefinitionsAndMaxTimestamp(ImmutableSet.of("nope"), maxTs))
                    .hasSize(0);
            assertThat(stateService.findByEventDefinitionsAndMaxTimestamp(ImmutableSet.of(id, "nope"), maxTs))
                    .hasSize(1);
        });
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void setState() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);

        // Before we set the state, there should be no record
        assertThat(stateService.findByEventDefinitionId("yolo")).isNotPresent();

        assertThat(stateService.setState("yolo", now.minusHours(1), now))
                .isPresent()
                .get()
                .satisfies(dto1 -> {
                    assertThat(dto1.minProcessedTimestamp()).isEqualTo(now.minusHours(1));
                    assertThat(dto1.maxProcessedTimestamp()).isEqualTo(now);
                    assertThat(dto1.eventDefinitionId()).isEqualTo("yolo");

                    assertThat(stateService.setState("yolo", now, now.plusHours(1)))
                            .isPresent()
                            .get()
                            .satisfies(dto2 -> {
                                // The second setState call should update the existing one
                                assertThat(dto2.id()).isEqualTo(dto1.id());
                                assertThat(dto2.eventDefinitionId()).isEqualTo("yolo");
                                assertThat(dto2.minProcessedTimestamp()).isEqualTo(dto1.minProcessedTimestamp());
                                assertThat(dto2.maxProcessedTimestamp()).isEqualTo(dto1.maxProcessedTimestamp().plusHours(1));
                            });
                });
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void setStateKeepsMinMaxTimestamp() {
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final DateTime min = now.minusHours(1);
        final DateTime max = now;

        // Before we set the state, there should be no record
        assertThat(stateService.findByEventDefinitionId("yolo")).isNotPresent();

        // Create state
        stateService.setState("yolo", min, now);

        // Check that it has been created
        assertThat(stateService.findByEventDefinitionId("yolo"))
                .isPresent()
                .get()
                .satisfies(dto -> {
                    assertThat(dto.minProcessedTimestamp()).isEqualTo(min);
                    assertThat(dto.maxProcessedTimestamp()).isEqualTo(now);
                });

        // Overwrite state with an EARLIER max timestamp
        stateService.setState("yolo", min, max.minusMinutes(10));

        // Max timestamp should NOT be overwritten by older timestamp
        assertThat(stateService.findByEventDefinitionId("yolo"))
                .isPresent()
                .get()
                .satisfies(dto -> {
                    assertThat(dto.minProcessedTimestamp()).isEqualTo(min);
                    assertThat(dto.maxProcessedTimestamp()).isEqualTo(max);
                });

        // Overwrite state with a LATER min timestamp
        stateService.setState("yolo", min.plusMinutes(5), max);

        // Min timestamp should NOT be overwritten by younger timestamp
        assertThat(stateService.findByEventDefinitionId("yolo"))
                .isPresent()
                .get()
                .satisfies(dto -> {
                    assertThat(dto.minProcessedTimestamp()).isEqualTo(min);
                    assertThat(dto.maxProcessedTimestamp()).isEqualTo(max);
                });

        // Overwrite state with a NEWER max timestamp
        stateService.setState("yolo", min, max.plusDays(10));

        // Max timestamp is now set to the newer one
        assertThat(stateService.findByEventDefinitionId("yolo"))
                .isPresent()
                .get()
                .satisfies(dto -> {
                    assertThat(dto.minProcessedTimestamp()).isEqualTo(min);
                    assertThat(dto.maxProcessedTimestamp()).isEqualTo(max.plusDays(10));
                });

        // Overwrite state with an OLDER min timestamp
        stateService.setState("yolo", min.minusDays(100), max.plusDays(10));

        // Min timestamp is now set to the older one
        assertThat(stateService.findByEventDefinitionId("yolo"))
                .isPresent()
                .get()
                .satisfies(dto -> {
                    assertThat(dto.minProcessedTimestamp()).isEqualTo(min.minusDays(100));
                    assertThat(dto.maxProcessedTimestamp()).isEqualTo(max.plusDays(10));
                });
    }

    @Test
    @UsingDataSet(locations = "event-processor-state.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteByEventProcessorId() {
        assertThat(stateService.deleteByEventDefinitionId("54e3deadbeefdeadbeefaff3")).isEqualTo(1);
        assertThat(stateService.deleteByEventDefinitionId("nope")).isEqualTo(0);
    }
}