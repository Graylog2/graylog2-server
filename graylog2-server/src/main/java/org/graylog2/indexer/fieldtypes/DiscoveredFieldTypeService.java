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
package org.graylog2.indexer.fieldtypes;

import org.graylog.plugins.views.search.ParameterProvider;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.engine.fieldlist.QueryAwareFieldListRetrievalParams;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;

import java.util.Set;

public interface DiscoveredFieldTypeService {

    String ALL_MESSAGE_FIELDS_DOCUMENT_FIELD = "gl2_message_fields";

    Set<MappedFieldTypeDTO> fieldTypesBySearch(Search search,
                                               QueryAwareFieldListRetrievalParams params);

    Set<MappedFieldTypeDTO> fieldTypesByQuery(Query query,
                                              ParameterProvider parameterProvider,
                                              QueryAwareFieldListRetrievalParams params);
}
