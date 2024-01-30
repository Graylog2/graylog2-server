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
package org.graylog.security.certutil;

import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.DataNodeStatus;
import org.graylog2.cluster.preflight.DataNodeProvisioningConfig;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CertRenewalService {

    record ProvisioningInformation(DataNodeProvisioningConfig.State status, String errorMsg,
                                   LocalDateTime certValidUntil) {
    }

    record DataNode(String nodeId, DataNodeStatus dataNodeStatus, String transportAddress,
                    DataNodeProvisioningConfig.State status, String errorMsg, String hostname, String shortNodeId,
                    LocalDateTime certValidUntil) {
        public String getId() {
            return nodeId;
        }
    }

    void checkCertificatesForRenewal();
    void initiateRenewalForNode(String nodeId);
    List<DataNode> findNodes();

    DataNodeDto addProvisioningInformation(DataNodeDto node);

    List<DataNodeDto> addProvisioningInformation(Collection<DataNodeDto> nodes);
}
