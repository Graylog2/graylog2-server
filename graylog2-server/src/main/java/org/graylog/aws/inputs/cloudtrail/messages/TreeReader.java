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
package org.graylog.aws.inputs.cloudtrail.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.graylog.aws.inputs.cloudtrail.json.CloudTrailRecord;
import org.graylog.aws.inputs.cloudtrail.json.CloudTrailRecordList;

import java.io.IOException;
import java.util.List;

public class TreeReader {
    private final ObjectMapper om;

    public TreeReader(ObjectMapper om) {
        this.om = om;
    }

    public List<CloudTrailRecord> read(String json) throws IOException {
        CloudTrailRecordList tree = om.readValue(json, CloudTrailRecordList.class);
        return ImmutableList.copyOf(tree.records);
    }
}
