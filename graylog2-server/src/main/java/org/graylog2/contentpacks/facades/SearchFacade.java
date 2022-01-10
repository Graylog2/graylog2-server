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
package org.graylog2.contentpacks.facades;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.ViewSummaryService;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;

public class SearchFacade extends ViewFacade {
    public static final ModelType TYPE_V1 = ModelTypes.SEARCH_V1;

    @Inject
    public SearchFacade(ObjectMapper objectMapper, SearchDbService searchDbService, ViewService viewService, ViewSummaryService viewSummaryService, UserService userService) {
        super(objectMapper, searchDbService, viewService, viewSummaryService, userService);
    }

    @Override
    public ModelType getModelType() {
        return TYPE_V1;
    }


    @Override
    public ViewDTO.Type getDTOType() {
        return ViewDTO.Type.SEARCH;
    }
}
