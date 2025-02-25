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
import React from 'react';

import { Col, Row } from 'components/bootstrap';

import ClusterConfigurationStatusLabel from './ClusterConfigurationStatusLabel';
import type { ClusterNode } from './useClusterNodes';

type Props = {
  graylogNodes: ClusterNode[],
  dataNodes: ClusterNode[],
}

const ClusterConfigurationClusterView = ({ graylogNodes, dataNodes }: Props) => (
  <Row style={{ marginTop: 10 }}>
    <Col md={3} sm={4} xs={6}>
      <h6><b>Graylog</b></h6>
      {graylogNodes.map((graylogNode) => (
        <div style={{ marginTop: 10, padding: 10, border: '1px solid', borderRadius: '10px' }}>
          <div>{graylogNode.nodeName}</div>
          <div><small>{graylogNode.role}</small></div>
          <ClusterConfigurationStatusLabel status={graylogNode.state} />
        </div>
      ))}
    </Col>
    <Col md={3} sm={4} xs={6}>
      <h6><b>Data Node</b></h6>
      {dataNodes.map((dataNode) => (
        <div style={{ marginTop: 10, padding: 10, border: '1px solid', borderRadius: '10px', width: '100%' }}>
          <div>{dataNode.nodeName}</div>
          <div><small>{dataNode.role}</small></div>
          <ClusterConfigurationStatusLabel status={dataNode.state} />
        </div>
      ))}
    </Col>
  </Row>
);

export default ClusterConfigurationClusterView;
