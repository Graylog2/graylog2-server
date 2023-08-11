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
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Timestamp } from 'components/common';
import { ClusterOverviewStore } from 'stores/cluster/ClusterOverviewStore';
import { NodesStore } from 'stores/nodes/NodesStore';
import { useStore } from 'stores/connect';

const ThreadDumpPage = () => {
  const { nodeId } = useParams();
  const { nodes } = useStore(NodesStore);
  const [threadDump, setThreadDump] = useState();
  const node = nodes?.[nodeId];

  useEffect(() => {
    if (nodeId) {
      ClusterOverviewStore.threadDump(nodeId).then((result) => setThreadDump(result));
    }
  }, [nodeId]);

  if (!node) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={`Thread dump of node ${node.short_node_id} / ${node.hostname}`}>
      <div>
        <PageHeader title={(
          <span>
            Thread dump of node {node.short_node_id} / {node.hostname}
            &nbsp;
            <small>Taken at <Timestamp dateTime={new Date()} /></small>
          </span>
        )} />
        <Row className="content">
          <Col md={12}>
            {threadDump
              ? <pre className="threaddump">{threadDump}</pre>
              : <Spinner />}
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default ThreadDumpPage;
