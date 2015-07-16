/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restclient.models.api.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import play.data.validation.Constraints;

import java.util.List;

@JsonAutoDetect
public class ExportBundleRequest extends ApiRequest {
    @JsonProperty("name")
    @Constraints.Required
    public String name;

    @JsonProperty("description")
    @Constraints.Required
    public String description;

    @JsonProperty("category")
    @Constraints.Required
    public String category;

    @JsonProperty("inputs")
    public List<String> inputs = Lists.newArrayList();

    @JsonProperty("outputs")
    public List<String> outputs = Lists.newArrayList();

    @JsonProperty("streams")
    public List<String> streams = Lists.newArrayList();

    @JsonProperty("dashboards")
    public List<String> dashboards = Lists.newArrayList();

    @JsonProperty("grok_patterns")
    public List<String> grokPatterns = Lists.newArrayList();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public void setInputs(List<String> inputs) {
        this.inputs = inputs;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }

    public List<String> getStreams() {
        return streams;
    }

    public void setStreams(List<String> streams) {
        this.streams = streams;
    }

    public List<String> getDashboards() {
        return dashboards;
    }

    public void setDashboards(List<String> dashboards) {
        this.dashboards = dashboards;
    }

    public List<String> getGrokPatterns() {
        return grokPatterns;
    }

    public void setGrokPatterns(List<String> grokPatterns) {
        this.grokPatterns = grokPatterns;
    }
}
