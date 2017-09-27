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

import org.graylog2.messageprocessors.MessageProcessorsConfig;
import org.graylog2.messageprocessors.StreamMatcherProcessor;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

public class V20170927170100_StreamMatcherFilter extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20170927170100_StreamMatcherFilter.class);

    private final Set<MessageProcessor.Descriptor> processorDescriptors;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public V20170927170100_StreamMatcherFilter(final Set<MessageProcessor.Descriptor> processorDescriptors,
                                               final ClusterConfigService clusterConfigService) {
        this.processorDescriptors = processorDescriptors;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2017-09-27T17:01:00Z");
    }

    @Override
    public void upgrade() {
        final StreamMatcherProcessor.Descriptor descriptor = new StreamMatcherProcessor.Descriptor();
        final MessageProcessorsConfig processorsConfig = clusterConfigService.get(MessageProcessorsConfig.class);
        if (processorsConfig == null) {
            LOG.debug("No message processor configuration found. Creating new one.");

            final Set<String> processorClassNames = processorDescriptors.stream()
                    .map(MessageProcessor.Descriptor::className)
                    .collect(Collectors.toSet());
            final MessageProcessorsConfig defaultProcessorsConfig = MessageProcessorsConfig.defaultConfig()
                    .withProcessors(processorClassNames);

            final LinkedList<String> processorOrder = new LinkedList<>(defaultProcessorsConfig.processorOrder());

            // Put stream matcher last
            processorOrder.remove(descriptor.className());
            processorOrder.addLast(descriptor.className());

            final MessageProcessorsConfig newProcessorsConfig = defaultProcessorsConfig.toBuilder()
                    .processorOrder(processorOrder)
                    .build();

            clusterConfigService.write(newProcessorsConfig);

            return;
        }

        if (processorsConfig.processorOrder().contains(descriptor.className())) {
            LOG.debug("Message processor configuration already contains stream matcher processor. Skipping migration.");
            return;
        }

        final LinkedList<String> newProcessorOrder = new LinkedList<>(processorsConfig.processorOrder());
        newProcessorOrder.addLast(descriptor.className());

        final MessageProcessorsConfig newProcessorsConfig = processorsConfig.toBuilder()
                .processorOrder(newProcessorOrder)
                .build();

        clusterConfigService.write(newProcessorsConfig);
    }
}
