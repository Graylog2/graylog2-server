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
package org.graylog2.indexer.datastream;

import java.util.Map;

public interface DataStreamService {

    /**
     * Create a data stream.
     * This will create a data stream template with the given mappings, adding a timestamp mapping if none
     * is specified for the timestampField in the mappings. Mappings will be made available to the FieldTypeMappingsService.
     * If no data stream with the given name exists, the data stream will be created.
     * A policy for handling lifecycle management of the data stream and its backing indices can be specified.
     * Existing templates, mappings and policies will be updated.
     *
     * @param dataStreamName name of the data stream
     * @param timestampField timestamp field
     * @param mappings       field type mappings in the data stream
     * @param ismPolicy      ism policy for backing index lifecycle management
     */
    void createDataStream(String dataStreamName, String timestampField, Map<String, Map<String, String>> mappings,
                          Policy ismPolicy);

}
