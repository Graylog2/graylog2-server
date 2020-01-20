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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.graylog.events.conditions.Expr;
import org.graylog.events.fields.providers.TemplateFieldValueProvider;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.events.processor.storage.PersistToStreamsStorageHandler;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class V20200102140000_UnifyEventSeriesIdTestIT {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private V20200102140000_UnifyEventSeriesId migration;
    private DBEventDefinitionService eventDefinitionService;

    @Mock
    private DBEventProcessorStateService dbEventProcessorStateService;

    @Mock
    private ClusterConfigService clusterConfigService;

    @Before
    public void setUp() throws Exception {
        final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        final ObjectMapper objectMapper = objectMapperProvider.get();
        objectMapper.registerSubtypes(new NamedType(AggregationEventProcessorConfig.class, AggregationEventProcessorConfig.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(TemplateFieldValueProvider.Config.class, TemplateFieldValueProvider.Config.TYPE_NAME));
        objectMapper.registerSubtypes(new NamedType(PersistToStreamsStorageHandler.Config.class, PersistToStreamsStorageHandler.Config.TYPE_NAME));
        final MongoJackObjectMapperProvider mapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        eventDefinitionService = new DBEventDefinitionService(mongodb.mongoConnection(), mapperProvider, dbEventProcessorStateService);

        migration = new V20200102140000_UnifyEventSeriesId(clusterConfigService, eventDefinitionService, objectMapperProvider);
    }

    @Test
    @MongoDBFixtures("V20200102140000_UnifyEventSeriesIdTestIT.json")
    public void testMigration() {
        assertThat(eventDefinitionService.streamAll().count()).isEqualTo(2);
        assertThat(eventDefinitionService.get("58458e442f857c314491344e").get()).satisfies(dto -> {
            assertThat(dto.config().type()).isEqualTo(AggregationEventProcessorConfig.TYPE_NAME);
            assertThat(dto.config()).satisfies(config -> {
                final AggregationEventProcessorConfig c = (AggregationEventProcessorConfig) config;
                assertThat(c.series().get(0).id()).isEqualTo("4711-2342");
                assertThat(c.conditions().get().expression().get())
                        .isEqualTo(Expr.Greater.create(Expr.NumberReference.create("4711-2342"), Expr.NumberValue.create(3.0)));
            });
        });

        migration.upgrade();

        assertThat(eventDefinitionService.streamAll().count()).isEqualTo(2);

        assertThat(eventDefinitionService.get("58458e442f857c314491344e").get()).satisfies(dto -> {
            assertThat(dto.config().type()).isEqualTo(AggregationEventProcessorConfig.TYPE_NAME);
            assertThat(dto.config()).satisfies(config -> {
                final AggregationEventProcessorConfig c = (AggregationEventProcessorConfig) config;
                assertThat(c.series().get(0).id()).isEqualTo("max-login_count");
                assertThat(c.conditions().get().expression().get())
                        .isEqualTo(Expr.Greater.create(Expr.NumberReference.create("max-login_count"), Expr.NumberValue.create(3.0)));
            });
        });

        assertThat(eventDefinitionService.get("5d3af98fdc820b587bc354bc").get()).satisfies(dto -> {
            assertThat(dto.config().type()).isEqualTo(AggregationEventProcessorConfig.TYPE_NAME);
            assertThat(dto.config()).satisfies(config -> {
                final AggregationEventProcessorConfig c = (AggregationEventProcessorConfig) config;
                assertThat(c.series().get(0).id()).isEqualTo("count-");
                assertThat(c.conditions().get().expression().get())
                        .isEqualTo(Expr.Greater.create(Expr.NumberReference.create("count-"), Expr.NumberValue.create(4.0)));
            });
        });
    }
}
