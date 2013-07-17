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
package org.graylog2.plugin.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.configuration.fields.ConfigurationField;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class ConfigurationRequest {

    private final List<ConfigurationField> fields;

    public ConfigurationRequest() {
        this.fields = Lists.newArrayList();
    }

    public void addField(ConfigurationField f) {
        fields.add(f);
    }

    public List<ConfigurationField> getFields() {
        return ImmutableList.copyOf(fields);
    }

    public List<Map<String, Object>> asList() {
        List<Map<String, Object>> configs = Lists.newArrayList();

        for (ConfigurationField f : fields) {
            Map<String, Object> config = Maps.newHashMap();
            config.put("type", f.getFieldType());
            config.put("name", f.getName());
            config.put("description", f.getDescription());
            config.put("default_value", f.getDefaultValue());
            config.put("is_optional", f.isOptional().equals(ConfigurationField.Optional.OPTIONAL));
            config.put("attributes", f.getAttributes());

            configs.add(config);
        }

        return configs;
    }

}
