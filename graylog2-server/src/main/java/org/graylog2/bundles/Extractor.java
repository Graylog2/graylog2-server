/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bundles;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Extractor {
    @JsonProperty
    private String title;
    @JsonProperty
    private org.graylog2.plugin.inputs.Extractor.Type type;
    @JsonProperty
    private org.graylog2.plugin.inputs.Extractor.CursorStrategy cursorStrategy;
    @JsonProperty
    private String targetField;
    @JsonProperty
    private String sourceField;
    @JsonProperty
    private Map<String, Object> configuration = Collections.emptyMap();
    @JsonProperty
    private List<Converter> converters;
    @JsonProperty
    private org.graylog2.plugin.inputs.Extractor.ConditionType conditionType;
    @JsonProperty
    private String conditionValue;
    @JsonProperty
    private int order;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public org.graylog2.plugin.inputs.Extractor.Type getType() {
        return type;
    }

    public void setType(org.graylog2.plugin.inputs.Extractor.Type type) {
        this.type = type;
    }

    public org.graylog2.plugin.inputs.Extractor.CursorStrategy getCursorStrategy() {
        return cursorStrategy;
    }

    public void setCursorStrategy(org.graylog2.plugin.inputs.Extractor.CursorStrategy cursorStrategy) {
        this.cursorStrategy = cursorStrategy;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public List<Converter> getConverters() {
        return converters;
    }

    public void setConverters(List<Converter> converters) {
        this.converters = converters;
    }

    public org.graylog2.plugin.inputs.Extractor.ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(org.graylog2.plugin.inputs.Extractor.ConditionType conditionType) {
        this.conditionType = conditionType;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
