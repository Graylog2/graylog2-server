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
import org.graylog2.restclient.models.bundles.Dashboard;
import org.graylog2.restclient.models.bundles.GrokPattern;
import org.graylog2.restclient.models.bundles.Input;
import org.graylog2.restclient.models.bundles.Output;
import org.graylog2.restclient.models.bundles.Stream;

import java.util.List;

@JsonAutoDetect
public class CreateBundleRequest extends ApiRequest {
    @JsonProperty("name")
    public String name;
    @JsonProperty("description")
    public String description;
    @JsonProperty("category")
    public String category;
    @JsonProperty("inputs")
    public List<Input> inputs;
    @JsonProperty("streams")
    public List<Stream> streams;
    @JsonProperty("outputs")
    public List<Output> outputs;
    @JsonProperty("dashboards")
    public List<Dashboard> dashboards;
    @JsonProperty("grok_patterns")
    public List<GrokPattern> grokPatterns;
}
