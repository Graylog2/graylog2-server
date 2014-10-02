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
package org.graylog2.restclient.lib.plugin.configuration;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class RequestedConfigurationField {

    private final String title;
    private final String humanName;
    private final Object defaultValue;
    private final String description;
    private final boolean isOptional;
    private final List<String> attributes;
    private final String superType;

    public RequestedConfigurationField(String superType, Map.Entry<String, Map<String, Object>> c) {
        this.title = c.getKey();
        Map<String, Object> info = c.getValue();

        if (!info.get("type").equals(superType)) {
            throw new RuntimeException("Type does not match supertype. This should never happen.");
        }

        this.superType = (String) info.get("type");

        this.humanName = (String) info.get("human_name");
        this.defaultValue = info.get("default_value");
        this.description = (String) info.get("description");
        this.isOptional = (Boolean) info.get("is_optional");
        this.attributes = (List<String>) info.get("attributes");
    }

    public String getTitle() {
        return title;
    }

    public String getHumanName() {
        return humanName;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public boolean hasAttribute(String attribute) {
        return attributes.contains(attribute.toLowerCase());
    }

    public String getAttributesAsJSValidationSpec() {
        StringBuilder sb = new StringBuilder();

        int i = 0;
        for (String attribute : attributes) {
            if (i > 0) {
                sb.append(" ");
            }

            sb.append(attributeToJSValidation(attribute));
            i++;
        }

        return sb.toString();
    }

    public abstract String getType();
    public abstract String attributeToJSValidation(String attribute);
}
