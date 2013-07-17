/**
 * Copyright (c) 2013 Lennart Koopmann <lennart@socketfeed.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.graylog2.plugin.configuration.fields;

import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class TextField implements ConfigurationField {

    public static final String FIELD_TYPE = "text";

    public enum Attribute {
        IS_PASSWORD,
        IS_SOCKET_ADDRESS
    }

    private final String name;
    private final String defaultValue;
    private final String description;
    private final Optional optional;

    private final List<String> attributes;

    public TextField(String name, String defaultValue, String description, Optional isOptional) {
        this(name, defaultValue, description, isOptional, null);
    }

    public TextField(String name, String defaultValue, String description, Attribute... attributes) {
        this(name, defaultValue, description, Optional.NOT_OPTIONAL, attributes);
    }

    public TextField(String name, String defaultValue, String description, Optional isOptional, Attribute... attrs) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.description = description;
        this.optional = isOptional;

        this.attributes = Lists.newArrayList();
        if (attrs != null) {
            for (Attribute attribute : attrs) {
                this.attributes.add(attribute.toString().toLowerCase());
            }
        }
    }

    @Override
    public String getFieldType() {
        return FIELD_TYPE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Optional isOptional() {
        return optional;
    }

    @Override
    public List<String> getAttributes() {
        return attributes;
    }

}
