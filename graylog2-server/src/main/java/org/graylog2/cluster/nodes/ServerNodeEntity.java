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
package org.graylog2.cluster.nodes;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.bson.types.ObjectId;
import org.graylog2.database.DbEntity;

import java.util.Map;

@DbEntity(collection = "nodes", titleField = "node_id")
public class ServerNodeEntity extends AbstractNode<ServerNodeDto> {

    @JsonCreator
    public ServerNodeEntity(Map<String, Object> fields) {
        super(fields);
    }

    public ServerNodeEntity(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public ServerNodeDto toDto() {
        return ServerNodeDto.Builder.builder()
                .setObjectId(this.getObjectId().toHexString())
                .setId(this.getNodeId())
                .setTransportAddress(this.getTransportAddress())
                .setLastSeen(this.getLastSeen())
                .setHostname(this.getHostname())
                .setLeader(this.isLeader())
                .build();
    }

}
