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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportBundle {
    @JsonProperty
    @NotBlank
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
    @NotBlank
    private String category;
    @JsonProperty
    @NotNull
    private List<String> inputs = Collections.emptyList();
    @JsonProperty
    @NotNull
    private List<String> streams = Collections.emptyList();
    @JsonProperty
    @NotNull
    private List<String> outputs = Collections.emptyList();
    @JsonProperty
    @NotNull
    private List<String> dashboards = Collections.emptyList();

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

    public List<String> getStreams() {
        return streams;
    }

    public void setStreams(List<String> streams) {
        this.streams = streams;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<String> outputs) {
        this.outputs = outputs;
    }

    public List<String> getDashboards() {
        return dashboards;
    }

    public void setDashboards(List<String> dashboards) {
        this.dashboards = dashboards;
    }
}
