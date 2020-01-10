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
package org.graylog2.outputs;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.journal.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

public class ElasticSearchOutput implements MessageOutput {
    private static final String WRITES_METRICNAME = name(ElasticSearchOutput.class, "writes");
    private static final String FAILURES_METRICNAME = name(ElasticSearchOutput.class, "failures");
    private static final String PROCESS_TIME_METRICNAME = name(ElasticSearchOutput.class, "processTime");

    private static final String NAME = "ElasticSearch Output";
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchOutput.class);

    private final Meter writes;
    private final Meter failures;
    private final Timer processTime;
    private final Messages messages;
    private final Journal journal;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @AssistedInject
    public ElasticSearchOutput(MetricRegistry metricRegistry,
                               Messages messages,
                               Journal journal,
                               @Assisted Stream stream,
                               @Assisted Configuration configuration) {
        this(metricRegistry, messages, journal);
    }

    @Inject
    public ElasticSearchOutput(MetricRegistry metricRegistry,
                               Messages messages,
                               Journal journal) {
        this.messages = messages;
        this.journal = journal;
        // Only constructing metrics here. write() get's another Core reference. (because this technically is a plugin)
        this.writes = metricRegistry.meter(WRITES_METRICNAME);
        this.failures = metricRegistry.meter(FAILURES_METRICNAME);
        this.processTime = metricRegistry.timer(PROCESS_TIME_METRICNAME);

        // Should be set in initialize once this becomes a real plugin.
        isRunning.set(true);
    }

    @Override
    public void write(Message message) throws Exception {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Writing message id to [{}]: <{}>", NAME, message.getId());
        }
        write(Collections.singletonList(message));
    }

    @Override
    public void write(List<Message> messageList) throws Exception {
        throw new UnsupportedOperationException("Method not supported!");
    }

    public void writeMessageEntries(List<Map.Entry<IndexSet, Message>> messageList) throws Exception {
        if (LOG.isTraceEnabled()) {
            final String sortedIds = messageList.stream()
                    .map(Map.Entry::getValue)
                    .map(Message::getId)
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.joining(", "));
            LOG.trace("Writing message ids to [{}]: <{}>", NAME, sortedIds);
        }

        writes.mark(messageList.size());
        final List<String> failedMessageIds;
        try (final Timer.Context ignored = processTime.time()) {
            failedMessageIds = messages.bulkIndex(messageList);
        }
        failures.mark(failedMessageIds.size());

        // TODO why doesn't this exclude the failedMessageIds?
        final Optional<Long> offset = messageList.stream()
            .map(Map.Entry::getValue)
            .map(Message::getJournalOffset)
            .max(Long::compare);

        offset.ifPresent(journal::markJournalOffsetCommitted);
    }

    @Override
    public void stop() {
        // TODO: Move ES stop code here.
        //isRunning.set(false);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    public interface Factory extends MessageOutput.Factory<ElasticSearchOutput> {
        @Override
        ElasticSearchOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            // Built in output. This is just for plugin compat. No special configuration required.
            return new ConfigurationRequest();
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Elasticsearch Output", false, "", "Elasticsearch Output");
        }

        public Descriptor(String name, boolean exclusive, String linkToDocs, String humanName) {
            super(name, exclusive, linkToDocs, humanName);
        }
    }
}
