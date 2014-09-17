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

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;

public class DashboardWidget {
    @JsonProperty
    @NotNull
    private String description;
    @JsonProperty
    @NotNull
    private org.graylog2.dashboards.widgets.DashboardWidget.Type type;
    @JsonProperty
    @Min(0)
    private int cacheTime = 10;
    @JsonProperty
    @NotNull
    private Map<String, Object> configuration = Collections.emptyMap();
    @JsonProperty
    @Min(0)
    private int col = -1;
    @JsonProperty
    @Min(0)
    private int row = -1;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public org.graylog2.dashboards.widgets.DashboardWidget.Type getType() {
        return type;
    }

    public void setType(org.graylog2.dashboards.widgets.DashboardWidget.Type type) {
        this.type = type;
    }

    public int getCacheTime() {
        return cacheTime;
    }

    public void setCacheTime(int cacheTime) {
        this.cacheTime = cacheTime;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }
}
