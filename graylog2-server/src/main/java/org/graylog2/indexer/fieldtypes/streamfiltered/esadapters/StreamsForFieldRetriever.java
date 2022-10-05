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
package org.graylog2.indexer.fieldtypes.streamfiltered.esadapters;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Returns streams that are used in documents where certain field is present/existing.
 */
public interface StreamsForFieldRetriever {

    Set<String> getStreams(final String fieldName, final String indexName);

    Map<String, Set<String>> getStreams(final List<String> fieldNames, final String indexName);
}
