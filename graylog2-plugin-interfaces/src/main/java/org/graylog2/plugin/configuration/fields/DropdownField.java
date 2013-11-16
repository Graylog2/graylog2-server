/**
 * Copyright (c) 2012 Lennart Koopmann <lennart@torch.sh>
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
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DropdownField implements ConfigurationField {

    public static final String FIELD_TYPE = "dropdown";

    private final String name;
    private final String humanName;
    private final String defaultValue;
    private final String description;
    private final Optional optional;
    private final Map<String, String> values;

    public DropdownField(String name, String humanName, String defaultValue, Map<String, String> values, Optional isOptional) {
        this(name, humanName, defaultValue, values, null, isOptional);
    }

    public DropdownField(String name, String humanName, String defaultValue, Map<String, String> values, String description, Optional isOptional) {
        this.name = name;
        this.humanName = humanName;
        this.defaultValue = defaultValue;
        this.description = description;
        this.optional = isOptional;
        this.values = values;
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
    public String getHumanName() {
        return humanName;
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
        return Lists.newArrayList();
    }

    @Override
    public Map<String, Map<String, String>> getAdditionalInformation() {
        Map<String, Map<String, String>> result = Maps.newHashMap();
        result.put("values", values);
        return result;
    }

    public static class ValueTemplates {

        public static Map<String, String> timeUnits() {
            Map<String, String> units = Maps.newHashMap();

            for(TimeUnit unit : TimeUnit.values()) {
                String human = unit.toString().toLowerCase();
                units.put(unit.toString(), Character.toUpperCase(human.charAt(0)) + human.substring(1));
            }

            return units;
        }

    }

}