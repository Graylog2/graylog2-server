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
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class BooleanField implements ConfigurationField {

    public static final String FIELD_TYPE = "boolean";

    private final String name;
    private final String humanName;
    private final boolean defaultValue;
    private final String description;

    public BooleanField(String name, String humanName, boolean defaultValue, String description) {
        this.name = name;
        this.humanName = humanName;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    @Override
    public String getFieldType() {
        return FIELD_TYPE;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getHumanName() {
        return humanName;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Optional isOptional() {
        // Always optional. If it's not checked it's false.
        return Optional.OPTIONAL;
    }

    @Override
    public List<String> getAttributes() {
        return Lists.newArrayList();
    }

    @Override
    public Map<String, Map<String, String>> getAdditionalInformation() {
        return Maps.newHashMap();
    }

}
