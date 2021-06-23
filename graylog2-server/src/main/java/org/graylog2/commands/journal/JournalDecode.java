/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.commands.journal;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import org.graylog2.inputs.codecs.CodecsModule;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ResolvableInetSocketAddress;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.ObjectMapperModule;
import org.graylog2.shared.journal.Journal;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.Map;

@Command(name = "decode", description = "Decodes messages from the journal")
public class JournalDecode extends AbstractJournalCommand {

    @Arguments(description = "Range of message offsets to decode, e.g. single number 1234567, upper bound ..123456, lower bound 123456..., both 123456..123458")
    @Required
    private String rangeArg;

    public JournalDecode() {
        super("decode-journal");
    }

    @Override
    protected List<Module> getCommandBindings() {
        return ImmutableList.<Module>builder()
                .addAll(super.getCommandBindings())
                .add(new CodecsModule())
                .add(new ObjectMapperModule(getClass().getClassLoader()))
                .build();
    }

    @Override
    protected void runCommand() {

        Range<Long> range;
        try {
            final List<String> offsets = Splitter.on("..").limit(2).splitToList(rangeArg);
            if (offsets.size() == 1) {
                range = Range.singleton(Long.valueOf(offsets.get(0)));
            } else if (offsets.size() == 2) {
                final String first = offsets.get(0);
                final String second = offsets.get(1);
                if (first.isEmpty()) {
                    range = Range.atMost(Long.valueOf(second));
                } else if (second.isEmpty()) {
                    range = Range.atLeast(Long.valueOf(first));
                } else {
                    range = Range.closed(Long.valueOf(first), Long.valueOf(second));
                }
            } else {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            System.err.println("Malformed offset range: " + rangeArg);
            return;
        }

        final Map<String, Codec.Factory<? extends Codec>> codecFactory =
                injector.getInstance(Key.get(new TypeLiteral<Map<String, Codec.Factory<? extends Codec>>>() {
                }));

        final Long readOffset = range.lowerEndpoint();
        final long count = range.upperEndpoint() - range.lowerEndpoint() + 1;
        final List<Journal.JournalReadEntry> entries = journal.read(readOffset,
                count);
        for (final Journal.JournalReadEntry entry : entries) {
            final RawMessage raw = RawMessage.decode(entry.getPayload(), entry.getOffset());
            if (raw == null) {
                System.err.println(MessageFormatter.format("Journal entry at offset {} failed to decode",
                        entry.getOffset()));
                continue;
            }

            final Codec codec = codecFactory.get(raw.getCodecName()).create(raw.getCodecConfig());
            final Message message = codec.decode(raw);
            if (message == null) {
                System.err.println(MessageFormatter.format(
                        "Could not use codec {} to decode raw message id {} at offset {}",
                        new Object[]{raw.getCodecName(), raw.getId(), entry.getOffset()}));
            } else {
                message.setMessageQueueId(raw.getMessageQueueId());
            }

            final ResolvableInetSocketAddress remoteAddress = raw.getRemoteAddress();
            final String remote = remoteAddress == null ? "unknown address" : remoteAddress.getInetSocketAddress().toString();

            final StringBuffer sb = new StringBuffer();
            sb.append("Message ").append(raw.getId()).append('\n')
                    .append(" at ").append(raw.getTimestamp()).append('\n')
                    .append(" in format ").append(raw.getCodecName()).append('\n')
                    .append(" at offset ").append(raw.getMessageQueueId()).append('\n')
                    .append(" received from remote address ").append(remote).append('\n')
                    .append(" (source field: ").append(message == null ? "unparsed" : message.getSource()).append(')').append('\n');
            if (message != null) {
                sb.append(" contains ").append(message.getFieldNames().size()).append(" fields.");
            } else {
                sb.append("The message could not be parse by the given codec.");
            }
            System.out.println(sb);
        }

    }
}
