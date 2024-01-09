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

@DbEntity(collection = "datanodes", titleField = "node_id")
public class DataNodeEntity extends AbstractNode<DataNodeDto> {

    @JsonCreator
    public DataNodeEntity(Map<String, Object> fields) {
        super(fields);
    }

    public DataNodeEntity(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    public String getClusterAddress() {
        return (String) fields.get("cluster_address");
    }

    public String getRestApiAddress() {
        return (String) fields.get("rest_api_address");
    }

    public DataNodeStatus getDataNodeStatus() {
        if (!fields.containsKey("datanode_status")) {
            return null;
        }
        return DataNodeStatus.valueOf(fields.get("datanode_status").toString());
    }

    @Override
    public DataNodeDto toDto() {
        return DataNodeDto.Builder.builder()
                .setObjectId(this.getObjectId().toHexString())
                .setId(this.getNodeId())
                .setTransportAddress(this.getTransportAddress())
                .setLastSeen(this.getLastSeen())
                .setHostname(this.getHostname())
                .setLeader(this.isLeader())
                .setClusterAddress(this.getClusterAddress())
                .setDataNodeStatus(this.getDataNodeStatus())
                .setRestApiAddress(this.getRestApiAddress())
                .build();
    }

}
