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
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.journal.Journal;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codahale.metrics.MetricRegistry.name;

public class DiscardMessageOutput implements MessageOutput {
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Journal journal;
    private final Meter messagesDiscarded;

    @AssistedInject
    public DiscardMessageOutput(final Journal journal,
                                final MetricRegistry metricRegistry,
                                @Assisted Stream stream,
                                @Assisted Configuration configuration) {
        this(journal, metricRegistry);
    }

    @Inject
    public DiscardMessageOutput(final Journal journal, final MetricRegistry metricRegistry) {
        this.journal = journal;
        this.messagesDiscarded = metricRegistry.meter(name(this.getClass(), "messagesDiscarded"));
        isRunning.set(true);
    }

    @Override
    public void stop() {
        isRunning.set(false);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        journal.markJournalOffsetCommitted(message.getJournalOffset());
        messagesDiscarded.mark();
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        long maxOffset = Long.MIN_VALUE;

        for (final Message message : messages) {
            maxOffset = Math.max(message.getJournalOffset(), maxOffset);
        }

        journal.markJournalOffsetCommitted(maxOffset);
        messagesDiscarded.mark(messages.size());
    }

    public interface Factory extends MessageOutput.Factory<DiscardMessageOutput> {
    }

    public static class Config extends MessageOutput.Config {
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Discard Message output", false, "", "Output that discards messages");
        }
    }
}
