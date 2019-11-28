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
/*
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


package org.graylog.plugins.views.search.errors;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ParameterExpansionError implements SearchError {
    private final String parameterName;
    private final String description;

    public ParameterExpansionError(String name) {
        this.parameterName = name;
        this.description = "Error while expanding parameter <" + parameterName + ">";
    }

    public ParameterExpansionError(String name, String msg) {
        this.parameterName = name;
        this.description = "Error while expanding parameter <" + parameterName + ">: " + msg;
    }

    @JsonProperty("parameter")
    public String parameterName() {
        return parameterName;
    }

    @Override
    public String description() {
        return description;
    }
}
