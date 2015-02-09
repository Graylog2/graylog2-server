/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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

public abstract class AbstractCodec implements Codec {
    private static final Logger log = LoggerFactory.getLogger(AbstractCodec.class);

    protected final Configuration configuration;

    private String name;

    protected AbstractCodec(Configuration configuration) {
        this.configuration = configuration;
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

            return configurationRequest;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
        }
    }
}
