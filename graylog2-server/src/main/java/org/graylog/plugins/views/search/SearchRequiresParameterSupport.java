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
package org.graylog.plugins.views.search;

import org.graylog.plugins.views.Requirement;
import org.graylog.plugins.views.search.views.EnterpriseMetadataSummary;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog.plugins.views.Requirement;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

public class SearchRequiresParameterSupport implements Requirement<Search> {
    public static final String Parameters = "parameters";

    private final EnterpriseMetadataSummary enterpriseMetadataSummary;

    @Inject
    public SearchRequiresParameterSupport(EnterpriseMetadataSummary enterpriseMetadataSummary) {
        this.enterpriseMetadataSummary = enterpriseMetadataSummary;
    }

    @Override
    public Map<String, PluginMetadataSummary> test(Search search) {
        return search.parameters().isEmpty()
                ? Collections.emptyMap()
                : Collections.singletonMap(Parameters, enterpriseMetadataSummary);
    }
}
