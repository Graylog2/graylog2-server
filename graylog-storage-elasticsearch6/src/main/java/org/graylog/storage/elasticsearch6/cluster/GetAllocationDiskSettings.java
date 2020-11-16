/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.storage.elasticsearch6.cluster;

import io.searchbox.cluster.GetSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class GetAllocationDiskSettings extends GetSettings {

    protected GetAllocationDiskSettings(Builder builder) {
        super(builder);
    }

    public static class Builder extends GetSettings.Builder {
        public Builder() {
            this.configureIncludeDefaults();
            this.configureSettingsFilter();
        }

        private void configureIncludeDefaults() {
            this.parameters.put("include_defaults", true);
        }

        private void configureSettingsFilter() {
            this.parameters.put("filter_path", this.getFilterPathValue());
        }

        private String getFilterPathValue() {
            List<String> filterPaths = new ArrayList<>();
            String commonFilterPath = "cluster.routing.allocation.disk";
            List<String> settingsGroup = Arrays.asList("defaults", "persistent", "transient");
            for (String settingGroup: settingsGroup) {
                filterPaths.add(String.join(".", settingGroup, commonFilterPath));
            }
            return String.join(",", filterPaths);
        }

        public GetAllocationDiskSettings build() {
            return new GetAllocationDiskSettings(this);
        }
    }
}
