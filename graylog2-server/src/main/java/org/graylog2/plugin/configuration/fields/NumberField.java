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
package org.graylog2.plugin.configuration.fields;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Locale;

public class NumberField extends AbstractConfigurationField {

    public static final String FIELD_TYPE = "number";

    public enum Attribute {
        ONLY_POSITIVE,
        ONLY_NEGATIVE,
        IS_PORT_NUMBER
    }

    private int defaultValue;

    private final List<String> attributes;

    public NumberField(String name, String humanName, int defaultValue, String description, Optional isOptional) {
        this(name, humanName, defaultValue, description, isOptional, new Attribute[0]);
    }

    public NumberField(String name, String humanName, int defaultValue, String description, Attribute... attributes) {
        this(name, humanName, defaultValue, description, Optional.NOT_OPTIONAL, attributes);
    }

    public NumberField(String name, String humanName, int defaultValue, String description, Optional isOptional, Attribute... attrs) {
        super(FIELD_TYPE, name, humanName, description, isOptional);
        this.defaultValue = defaultValue;

        this.attributes = Lists.newArrayList();
        if (attrs != null) {
            for (Attribute attribute : attrs) {
                this.attributes.add(attribute.toString().toLowerCase(Locale.ENGLISH));
            }
        }
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof Integer) {
            this.defaultValue = (int) defaultValue;
        }
    }

    @Override
    public List<String> getAttributes() {
        return attributes;
    }
}
