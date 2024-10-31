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
import org.graylog2.datanode.DataNodeLifecycleTrigger;
import org.joda.time.DateTime;
import org.joda.time.base.AbstractInstant;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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

    public Date getCertValidTill() {
        final DateTime dateTime = (DateTime) fields.get(DataNodeDto.FIELD_CERT_VALID_UNTIL);
        return Optional.ofNullable(dateTime).map(AbstractInstant::toDate).orElse(null);
    }

    public DataNodeLifecycleTrigger getActionQueue() {
        if (!fields.containsKey("action_queue") || Objects.isNull(fields.get("action_queue"))) {
            return null;
        }
        return DataNodeLifecycleTrigger.valueOf(fields.get("action_queue").toString());
    }

    public DataNodeStatus getDataNodeStatus() {
        if (!fields.containsKey("datanode_status")) {
            return null;
        }
        return DataNodeStatus.valueOf(fields.get("datanode_status").toString());
    }


    public String getDatanodeVersion() {
        if (!fields.containsKey(DataNodeDto.FIELD_DATANODE_VERSION)) {
            return null;
        }
        return (String) fields.get(DataNodeDto.FIELD_DATANODE_VERSION);
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
                .setActionQueue(this.getActionQueue())
                .setCertValidUntil(this.getCertValidTill())
                .setDatanodeVersion(this.getDatanodeVersion())
                .build();
    }

}
