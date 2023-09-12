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
package org.graylog2.inputs.codecs;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.MultiMessageCodec;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Codec(name = CSVCodec.NAME, displayName = "CSV Codec")
public class CSVCodec extends AbstractCodec implements MultiMessageCodec {
    private static final Logger LOG = LoggerFactory.getLogger(CSVCodec.class);
    public static final String NAME = "csvcodec";
    public static final String CK_SOURCE = "source";
    public static final String CK_URL = "target_url";
    public static final String CK_DELIMITER = "set_delimiter";
    private final String delimiter;

    @AssistedInject
    protected CSVCodec(@Assisted Configuration configuration) {
        super(configuration);
        delimiter = configuration.getString(CK_DELIMITER);
    }

    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        return null;
    }

    @Override
    public Collection<Message> decodeMessages(@Nonnull RawMessage rawMessage) {
        try {
            return parseCsv(rawMessage.getPayload(), delimiter.charAt(0))
                    .map(csvRow -> {
                        final Message message = new Message(rawMessage.getTimestamp() + " from: " + configuration.getString(CK_URL),
                                configuration.getString(CK_SOURCE),
                                rawMessage.getTimestamp());
                        message.addFields(csvRow);
                        return message;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.warn("Could not parse CSV", e);
            throw new RuntimeException("Could not parse CSV", e);
        }
    }

    private Stream<Map<String, Object>> parseCsv(byte[] payload, char delimiter) throws IOException {
        CSVParser response = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withDelimiter(delimiter)
                .parse(new InputStreamReader(new ByteArrayInputStream(payload), charset));

        List<String> headerNames = response.getHeaderNames();

        return StreamSupport.stream(response.spliterator(), false)
                .map(strings -> headerNames.stream().collect(Collectors.toMap(Function.identity(), strings::get)));

    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<CSVCodec> {
        @Override
        CSVCodec create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            r.addField(new TextField(
                    CK_SOURCE,
                    "Message source",
                    "yourapi",
                    "What to use as source field of the resulting message.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));
            r.addField(new TextField(
                    CK_URL,
                    "URL of CSV resource",
                    "youruri",
                    "HTTP resource returning CSV on GET.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));
            r.addField(new TextField(
                    CK_DELIMITER,
                    "Delimiter of CSV resource",
                    ",",
                    "The delimiter is what separates the values in the CSV. The Field only accepts single characters.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            return r;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {

        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(CSVCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
