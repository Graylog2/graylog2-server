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
package org.graylog2.restclient.models.api.requests.outputs;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.restclient.models.api.requests.ApiRequest;
import play.data.validation.Constraints;

import java.util.List;
import java.util.Map;

public class OutputLaunchRequest extends ApiRequest {
    @Constraints.Required
    public String title;
    @Constraints.Required
    public String type;
    public Map<String, Object> configuration;
    @JsonProperty("streams")
    public List<String> streams;

    @Override
    public String toString() {
        return "OutputLaunchRequest{" +
                "title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", configuration=" + configuration +
                ", streams=" + streams +
                '}';
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public List<String> getStreams() {
        return streams;
    }

    public void setStreams(List<String> streams) {
        this.streams = streams;
    }
}
