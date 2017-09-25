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
package org.graylog2.bundles;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Set;

@JsonAutoDetect
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
    @JsonProperty
    private Set<GrokPattern> grokPatterns = Collections.emptySet();
    @JsonProperty
    private Set<LookupTableBundle> lookupTables = Collections.emptySet();
    @JsonProperty
    private Set<LookupCacheBundle> lookupCaches = Collections.emptySet();
    @JsonProperty
    private Set<LookupDataAdapterBundle> lookupDataAdapters = Collections.emptySet();

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

    public Set<GrokPattern> getGrokPatterns() {
        return grokPatterns;
    }

    public void setGrokPatterns(Set<GrokPattern> grokPatterns) {
        this.grokPatterns = grokPatterns;
    }

    public Set<LookupTableBundle> getLookupTables() {
        return lookupTables;
    }

    public void setLookupTables(Set<LookupTableBundle> lookupTables) {
        this.lookupTables = lookupTables;
    }

    public Set<LookupCacheBundle> getLookupCaches() {
        return lookupCaches;
    }

    public void setLookupCaches(Set<LookupCacheBundle> lookupCaches) {
        this.lookupCaches = lookupCaches;
    }

    public Set<LookupDataAdapterBundle> getLookupDataAdapters() {
        return lookupDataAdapters;
    }

    public void setLookupDataAdapters(Set<LookupDataAdapterBundle> lookupDataAdapters) {
        this.lookupDataAdapters = lookupDataAdapters;
    }
}
