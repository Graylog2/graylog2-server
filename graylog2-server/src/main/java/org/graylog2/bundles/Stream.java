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
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@JsonAutoDetect
public class Stream {
    @JsonProperty
    private String id;
    @JsonProperty
    @NotBlank
    private String title;
    @JsonProperty
    private String description;
    @JsonProperty
    private boolean disabled = false;
    @JsonProperty
    private org.graylog2.plugin.streams.Stream.MatchingType matchingType = org.graylog2.plugin.streams.Stream.MatchingType.DEFAULT;
    @JsonProperty
    @NotNull
    private List<StreamRule> streamRules = Collections.emptyList();
    @JsonProperty
    @NotNull
    private Set<String> outputs = Collections.emptySet();
    @JsonProperty
    private boolean defaultStream = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public org.graylog2.plugin.streams.Stream.MatchingType getMatchingType() {
        return matchingType;
    }

    public void setMatchingType(org.graylog2.plugin.streams.Stream.MatchingType matchingType) {
        this.matchingType = matchingType;
    }

    public List<StreamRule> getStreamRules() {
        return streamRules;
    }

    public void setStreamRules(List<StreamRule> streamRules) {
        this.streamRules = streamRules;
    }

    public Set<String> getOutputs() {
        return outputs;
    }

    public void setOutputs(Set<String> outputs) {
        this.outputs = outputs;
    }

    public boolean isDefaultStream() {
        return defaultStream;
    }

    public void setDefaultStream(boolean defaultStream) {
        this.defaultStream = defaultStream;
    }
}
