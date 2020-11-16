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

import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.views.Requirement;
import org.graylog.plugins.views.Requirements;

import javax.inject.Inject;
import java.util.Set;

public class SearchRequirements extends Requirements<Search> {
    @Inject
    public SearchRequirements(Set<Requirement<Search>> requirements, @Assisted Search dto) {
        super(requirements, dto);
    }

    public interface Factory extends Requirements.Factory<Search> {
        SearchRequirements create(Search dto);
    }
}
