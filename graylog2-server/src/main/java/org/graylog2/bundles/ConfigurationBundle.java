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
import org.mongojack.ObjectId;

import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigurationBundle {
    @Id
    @ObjectId
    @JsonProperty
    private String id;
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
    private Set<Input> inputs = Collections.emptySet();
    @JsonProperty
    @NotNull
    private Set<Stream> streams = Collections.emptySet();
    @JsonProperty
    @NotNull
    private Set<Output> outputs = Collections.emptySet();
    @JsonProperty
    @NotNull
    private Set<Dashboard> dashboards = Collections.emptySet();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Set<Input> getInputs() {
        return inputs;
    }

    public void setInputs(Set<Input> inputs) {
        this.inputs = inputs;
    }

    public Set<Stream> getStreams() {
        return streams;
    }

    public void setStreams(Set<Stream> streams) {
        this.streams = streams;
    }

    public Set<Output> getOutputs() {
        return outputs;
    }

    public void setOutputs(Set<Output> outputs) {
        this.outputs = outputs;
    }

    public Set<Dashboard> getDashboards() {
        return dashboards;
    }

    public void setDashboards(Set<Dashboard> dashboards) {
        this.dashboards = dashboards;
    }
}
