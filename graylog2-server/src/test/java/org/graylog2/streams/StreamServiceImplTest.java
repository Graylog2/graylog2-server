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
package org.graylog2.streams;

import com.google.common.collect.ImmutableSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.bson.types.ObjectId;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alerts.AlertService;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StreamServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StreamRuleService streamRuleService;
    @Mock
    private AlertService alertService;
    @Mock
    private OutputService outputService;
    @Mock
    private IndexSetService indexSetService;
    @Mock
    private MongoIndexSet.Factory factory;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AlarmCallbackConfigurationService alarmCallbackConfigurationService;

    private StreamService streamService;

    @Before
    public void setUp() throws Exception {
        this.streamService = new StreamServiceImpl(mongoRule.getMongoConnection(), streamRuleService, alertService,
            outputService, indexSetService, factory, notificationService, new ClusterEventBus(), alarmCallbackConfigurationService);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void loadAllWithConfiguredAlertConditionsShouldNotFailWhenNoStreamsArePresent() {
        final List<Stream> alertableStreams = this.streamService.loadAllWithConfiguredAlertConditions();

        assertThat(alertableStreams)
            .isNotNull()
            .isEmpty();
    }

    @Test
    @UsingDataSet(locations = "someStreamsWithoutAlertConditions.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loadAllWithConfiguredAlertConditionsShouldReturnNoStreams() {
        final List<Stream> alertableStreams = this.streamService.loadAllWithConfiguredAlertConditions();

        assertThat(alertableStreams)
            .isEmpty();
    }

    @Test
    @UsingDataSet(locations = {"someStreamsWithoutAlertConditions.json", "someStreamsWithAlertConditions.json"}, loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loadAllWithConfiguredAlertConditionsShouldReturnStreams() {
        final List<Stream> alertableStreams = this.streamService.loadAllWithConfiguredAlertConditions();

        assertThat(alertableStreams)
            .isNotEmpty()
            .hasSize(2);
    }

    @Test
    @UsingDataSet(locations = "someStreamsWithAlertConditions.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void loadByIds() {
        assertThat(this.streamService.loadByIds(ImmutableSet.of("565f02223b0c25a537197af2"))).hasSize(1);
        assertThat(this.streamService.loadByIds(ImmutableSet.of("565f02223b0c25a5deadbeef"))).isEmpty();
        assertThat(this.streamService.loadByIds(ImmutableSet.of("565f02223b0c25a537197af2", "565f02223b0c25a5deadbeef"))).hasSize(1);
    }

    @Test
    @UsingDataSet(locations = "someStreamsWithoutAlertConditions.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void addOutputs() throws NotFoundException {
        final ObjectId streamId = new ObjectId("5628f4503b0c5756a8eebc4d");
        final ObjectId output1Id = new ObjectId("5628f4503b00deadbeef0001");
        final ObjectId output2Id = new ObjectId("5628f4503b00deadbeef0002");

        final Output output1 = mock(Output.class);
        final Output output2 = mock(Output.class);

        when(output1.getId()).thenReturn(output1Id.toHexString());
        when(output2.getId()).thenReturn(output2Id.toHexString());
        when(outputService.load(output1Id.toHexString())).thenReturn(output1);
        when(outputService.load(output2Id.toHexString())).thenReturn(output2);

        streamService.addOutputs(streamId, ImmutableSet.of(output1Id, output2Id));

        final Stream stream = streamService.load(streamId.toHexString());
        assertThat(stream.getOutputs())
                .anySatisfy(output -> assertThat(output.getId()).isEqualTo(output1Id.toHexString()))
                .anySatisfy(output -> assertThat(output.getId()).isEqualTo(output2Id.toHexString()));
    }
}
