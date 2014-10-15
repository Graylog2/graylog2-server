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
package org.graylog2.restclient.models.bundles;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import javax.validation.constraints.NotNull;
import java.util.List;

public class ConfigurationBundle {
    @JsonProperty
    private String id;

    @JsonProperty
    @NotNull
    private String name;

    @JsonProperty
    @NotNull
    private String description;

    @JsonProperty
    @NotNull
    private String category;

    @JsonProperty
    private List<Input> inputs = Lists.newArrayList();

    @JsonProperty
    private List<Stream> streams = Lists.newArrayList();

    @JsonProperty
    private List<Output> outputs = Lists.newArrayList();

    @JsonProperty
    private List<Dashboard> dashboards = Lists.newArrayList();

}