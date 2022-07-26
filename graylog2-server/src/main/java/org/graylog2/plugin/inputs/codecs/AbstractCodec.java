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
package org.graylog2.plugin.inputs.codecs;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class AbstractCodec implements Codec {
    private static final Logger log = LoggerFactory.getLogger(AbstractCodec.class);

    protected final Configuration configuration;
    protected final Charset charset;
    private String name;

    protected AbstractCodec(Configuration configuration) {
        this.configuration = configuration;
        if (configuration.stringIsSet(Codec.Config.CK_CHARSET_NAME)) {
            this.charset = Charset.forName(configuration.getString(Codec.Config.CK_CHARSET_NAME));
        }
        else {
            this.charset = StandardCharsets.UTF_8;
        }
    }

    @Override
    @Nonnull
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public String getName() {
        // can be a race condition, but we don't care the outcome is always the same
        if (name == null) {
            if (this.getClass().isAnnotationPresent(org.graylog2.plugin.inputs.annotations.Codec.class)) {
                name = this.getClass().getAnnotation(org.graylog2.plugin.inputs.annotations.Codec.class).name();
            } else {
                log.error("Annotation {} missing on codec {}. This is a bug and this codec will not be available.",
                          org.graylog2.plugin.inputs.annotations.Codec.class, this.getClass());
            }
        }
        return name;
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    public abstract static class Config implements Codec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configurationRequest = new ConfigurationRequest();

            // TODO implement universal override (in raw message maybe?)
            configurationRequest.addField(new TextField(
                    CK_OVERRIDE_SOURCE,
                    "Override source",
                    null,
                    "The source is a hostname derived from the received packet by default. Set this if you want to override " +
                            "it with a custom string.",
                    ConfigurationField.Optional.OPTIONAL
            ));

            configurationRequest.addField(new TextField(
                    CK_CHARSET_NAME,
                    "Encoding",
                    StandardCharsets.UTF_8.name(),
                    "Default encoding is UTF-8. Set this to a standard charset name if you want override the default.",
                    ConfigurationField.Optional.OPTIONAL
            ));

            return configurationRequest;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
        }
    }

    public static class Descriptor extends Codec.Descriptor {
        public Descriptor(String name) {
            super(name);
        }
    }
}
