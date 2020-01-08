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
package org.graylog2.plugin.configuration;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationRequestTest {
    private ConfigurationRequest configurationRequest;

    @Before
    public void setUp() throws Exception {
        configurationRequest = new ConfigurationRequest();
    }

    @Test
    public void putAllRetainsOrder() throws Exception {
        final ImmutableMap<String, ConfigurationField> fields = ImmutableMap.<String, ConfigurationField>of(
                "field1", new TextField("field1", "humanName", "defaultValue", "description"),
                "field2", new TextField("field2", "humanName", "defaultValue", "description"),
                "field3", new TextField("field3", "humanName", "defaultValue", "description")
        );
        configurationRequest.putAll(fields);

        assertThat(configurationRequest.getFields().keySet()).containsSequence("field1", "field2", "field3");
    }

    @Test
    public void addFieldAppendsFieldAtTheEnd() throws Exception {
        int numberOfFields = 5;
        for (int i = 0; i < numberOfFields; i++) {
            configurationRequest.addField(new TextField("field" + i, "humanName", "defaultValue", "description"));
        }

        assertThat(configurationRequest.getFields().keySet())
                .containsSequence("field0", "field1", "field2", "field3", "field4");
    }

    @Test
    public void asListRetainsOrder() throws Exception {
        int numberOfFields = 5;
        for (int i = 0; i < numberOfFields; i++) {
            configurationRequest.addField(new TextField("field" + i, "humanName", "defaultValue", "description"));
        }

        assertThat(configurationRequest.asList().keySet())
                .containsSequence("field0", "field1", "field2", "field3", "field4");
    }
}
