/**
 * Copyright (C) 2012, 2013, 2014 wasted.io Ltd <really@wasted.io>
 * Copyright (C) 2015-2017 Graylog, Inc. (hello@graylog.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.plugins.netflow.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.SystemUtils;
import org.graylog.plugins.netflow.flows.FlowException;
import org.graylog.plugins.netflow.flows.NetFlowParser;
import org.graylog.plugins.netflow.v9.NetFlowV9FieldTypeRegistry;
import org.graylog.plugins.netflow.v9.NetFlowV9TemplateCache;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.MultiMessageCodec;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.ScheduledExecutorService;

@Codec(name = "netflow", displayName = "NetFlow")
public class NetFlowCodec extends AbstractCodec implements MultiMessageCodec {
    private static final Logger LOG = LoggerFactory.getLogger(NetFlowCodec.class);

    @VisibleForTesting
    static final String CK_CACHE_SIZE = "cache_size";
    @VisibleForTesting
    static final String CK_CACHE_PATH = "cache_path";
    @VisibleForTesting
    static final String CK_CACHE_SAVE_INTERVAL = "cache_save_interval";

    private static final int DEFAULT_CACHE_SIZE = 1000;
    private static final String DEFAULT_CACHE_PATH = SystemUtils.getJavaIoTmpDir().toPath().resolve("netflow-templates.json").toString();
    private static final int DEFAULT_CACHE_SAVE_INTERVAL = 15 * 60;

    private final NetFlowV9TemplateCache templateCache;
    private final NetFlowV9FieldTypeRegistry typeRegistry = new NetFlowV9FieldTypeRegistry();

    @Inject
    protected NetFlowCodec(@Assisted Configuration configuration,
                           @Named("daemonScheduler") ScheduledExecutorService scheduler,
                           ObjectMapper objectMapper) {
        super(configuration);

        final int cacheSize = configuration.getInt(CK_CACHE_SIZE, DEFAULT_CACHE_SIZE);
        final int cacheSaveInterval = configuration.getInt(CK_CACHE_SAVE_INTERVAL, DEFAULT_CACHE_SAVE_INTERVAL);
        final String configCachePath = configuration.getString(CK_CACHE_PATH, DEFAULT_CACHE_PATH);
        final Path cachePath = Paths.get(configCachePath);

        templateCache = new NetFlowV9TemplateCache(cacheSize, cachePath, cacheSaveInterval, scheduler, objectMapper);
    }

    @Nullable
    @Override
    public Message decode(@Nonnull RawMessage rawMessage) {
        throw new UnsupportedOperationException("MultiMessageCodec " + getClass() + " does not support decode()");
    }

    @Nullable
    @Override
    public Collection<Message> decodeMessages(@Nonnull RawMessage rawMessage) {
        try {
            return NetFlowParser.parse(rawMessage, templateCache, typeRegistry);
        } catch (FlowException e) {
            LOG.error("Error parsing NetFlow packet", e);
            return null;
        }
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<NetFlowCodec> {
        @Override
        NetFlowCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(2055);
            }
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configuration = super.getRequestedConfiguration();

            configuration.addField(new NumberField(CK_CACHE_SIZE, "Maximum cache size", DEFAULT_CACHE_SIZE, "Maximum number of elements in the NetFlow9 template cache", ConfigurationField.Optional.OPTIONAL));
            configuration.addField(new TextField(CK_CACHE_PATH, "Cache file path", DEFAULT_CACHE_PATH, "Path to the file persisting the the NetFlow9 template cache", ConfigurationField.Optional.OPTIONAL));
            configuration.addField(new NumberField(CK_CACHE_SAVE_INTERVAL, "Cache save interval (seconds)", DEFAULT_CACHE_SAVE_INTERVAL, "Interval in seconds for persisting the cache contents", ConfigurationField.Optional.OPTIONAL));

            return configuration;
        }
    }
}
