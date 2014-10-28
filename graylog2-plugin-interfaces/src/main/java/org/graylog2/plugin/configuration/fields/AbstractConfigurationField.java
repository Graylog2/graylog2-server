/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
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
package org.graylog2.plugin.configuration.fields;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractConfigurationField implements ConfigurationField {
    protected final String field_type;
    protected final String name;
    protected final String humanName;
    protected final String description;
    protected final ConfigurationField.Optional optional;

    public AbstractConfigurationField(String field_type, String name, String humanName, String description, ConfigurationField.Optional optional1) {
        this.field_type = field_type;
        this.name = name;
        this.humanName = humanName;
        this.description = description;
        this.optional = optional1;
    }

    public String getFieldType() {
        return field_type;
    }

    public ConfigurationField.Optional isOptional() {
        return optional;
    }

    public String getName() {
        return name;
    }

    public String getHumanName() {
        return humanName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAttributes() {
        return Collections.emptyList();
    }

    public Map<String, Map<String, String>> getAdditionalInformation() {
        return Collections.emptyMap();
    }
}
