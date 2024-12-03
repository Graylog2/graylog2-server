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

type Props = {
  graylogNodes: any,
  dataNodes: any,
}

const ClusterConfigurationClusterView = ({ graylogNodes, dataNodes }: Props) => {
  return (
    <Row style={{ marginTop: 10 }}>
      <Col md={6}>
        <h6><b>Graylog</b></h6>
        {graylogNodes.map((graylogNode) => (
          <div style={{ margin: 10, padding: 10, border: '1px solid', borderRadius: '10px' }}>{graylogNode.nodeName}</div>
        ))}
      </Col>
      <Col md={6}>
        <h6><b>Data Node</b></h6>
        {dataNodes.map((dataNode) => (
          <div style={{ margin: 10, padding: 10, border: '1px solid', borderRadius: '10px' }}>{dataNode.nodeName}</div>
        ))}
      </Col>
    </Row>
  );
};

export default ClusterConfigurationClusterView;
