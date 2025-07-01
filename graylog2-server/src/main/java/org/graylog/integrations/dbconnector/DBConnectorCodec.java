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
package org.graylog.integrations.dbconnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_DATABASE_NAME;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_HOSTNAME;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_MONGO_COLLECTION_NAME;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_OVERRIDE_SOURCE;
import static org.graylog.integrations.dbconnector.DBConnectorInput.CK_TABLE_NAME;
import static org.graylog.integrations.dbconnector.DBConnectorInput.Config.addConnectionFields;

public class DBConnectorCodec extends AbstractCodec {
    private static final Logger LOG = LoggerFactory.getLogger(DBConnectorCodec.class);

    public static final String CK_STORE_FULL_MESSAGE = "store_full_message";
    public static final String NAME = "DBConnectorCodec";
    private final ObjectMapper objectMapper;
    private final MessageFactory messageFactory;

    @Inject
    protected DBConnectorCodec(@Assisted Configuration configuration, ObjectMapper objectMapper, MessageFactory messageFactory) {
        super(configuration);
        this.objectMapper = objectMapper;
        this.messageFactory = messageFactory;
    }

    @Nullable
    @Override
    public Optional<Message> decodeSafe(@Nonnull RawMessage rawMessage) {
        try {
            LOG.debug("Attempting to decode Database Connector logs.");
            Map<String, Object> map = objectMapper.readValue(rawMessage.getPayload(), TypeReferences.MAP_STRING_OBJECT);
            final String source = getSource();
            Message message = messageFactory.createMessage(new String(rawMessage.getPayload(), StandardCharsets.UTF_8), source, rawMessage.getTimestamp());
            message.setSourceInputId(rawMessage.getSourceNodes().get(0).inputId);
            message.addFields(map);
            final String overrideSourceValue = configuration.getString(CK_OVERRIDE_SOURCE);
            if (StringUtils.isNotBlank(overrideSourceValue)) {
                message.setSource(overrideSourceValue);
            }
            return Optional.of(message);

        } catch (IOException e) {
            LOG.error("Failed to decode Database Connector log event.", e);
            return Optional.empty();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @FactoryClass
    public interface Factory extends Codec.Factory<DBConnectorCodec> {
        @Override
        DBConnectorCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = new ConfigurationRequest();

            addConnectionFields(r);

            return r;
        }
    }

    public String getSource() {
        String hostname = configuration.getString(CK_HOSTNAME);
        String databaseName = configuration.getString(CK_DATABASE_NAME);
        String tableName = configuration.getString(CK_TABLE_NAME);
        String collectionName = configuration.getString(CK_MONGO_COLLECTION_NAME);
        return hostname + " | " + databaseName + " | " + (StringUtils.isNotBlank(tableName) ? tableName : collectionName);

    }
}
