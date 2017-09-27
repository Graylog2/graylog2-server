package org.graylog2.migrations;

import org.graylog2.messageprocessors.MessageProcessorsConfig;
import org.graylog2.messageprocessors.StreamMatcherProcessor;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class V20170927170100_StreamMatcherFilterTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClusterConfigService clusterConfigService;

    private V20170927170100_StreamMatcherFilter migration;

    @Before
    public void setUp() throws Exception {
        final Set<MessageProcessor.Descriptor> descriptors = Collections.singleton(new MessageProcessor.Descriptor() {
            @Override
            public String name() {
                return "Some Message Processor";
            }

            @Override
            public String className() {
                return "SomeMessageProcessor";
            }
        });
        migration = new V20170927170100_StreamMatcherFilter(descriptors, clusterConfigService);
    }

    @Test
    public void upgrade_creates_new_MessageProcessorsConfig_if_none_exists() throws Exception {
        when(clusterConfigService.get(MessageProcessorsConfig.class)).thenReturn(null);

        migration.upgrade();

        final ArgumentCaptor<MessageProcessorsConfig> argument = ArgumentCaptor.forClass(MessageProcessorsConfig.class);
        verify(clusterConfigService, times(1)).write(argument.capture());

        final MessageProcessorsConfig processorsConfig = argument.getValue();
        final StreamMatcherProcessor.Descriptor StreamMatcherProcessorDescriptor = new StreamMatcherProcessor.Descriptor();
        assertThat(processorsConfig.processorOrder()).containsExactly("SomeMessageProcessor", StreamMatcherProcessorDescriptor.className());
    }

    @Test
    public void upgrade_modifies_MessageProcessorsConfig_if_StreamMatcherProcessor_is_missing() throws Exception {
        final MessageProcessorsConfig config = MessageProcessorsConfig.create(Collections.singletonList("Foobar"));
        when(clusterConfigService.get(MessageProcessorsConfig.class)).thenReturn(config);

        migration.upgrade();

        final ArgumentCaptor<MessageProcessorsConfig> argument = ArgumentCaptor.forClass(MessageProcessorsConfig.class);
        verify(clusterConfigService, times(1)).write(argument.capture());

        final MessageProcessorsConfig processorsConfig = argument.getValue();
        final StreamMatcherProcessor.Descriptor StreamMatcherProcessorDescriptor = new StreamMatcherProcessor.Descriptor();
        assertThat(processorsConfig.processorOrder()).containsExactly("Foobar", StreamMatcherProcessorDescriptor.className());
    }

    @Test
    public void upgrade_does_nothing_if_MessageProcessorsConfig_contains_StreamMatcherProcessor() throws Exception {
        final StreamMatcherProcessor.Descriptor streamMatcherProcessorDescriptor = new StreamMatcherProcessor.Descriptor();
        final MessageProcessorsConfig config = MessageProcessorsConfig.create(
                Arrays.asList("Foobar", streamMatcherProcessorDescriptor.className(), "Quux"));
        when(clusterConfigService.get(MessageProcessorsConfig.class)).thenReturn(config);

        migration.upgrade();

        verify(clusterConfigService, never()).write(any(MessageProcessorsConfig.class));
    }
}