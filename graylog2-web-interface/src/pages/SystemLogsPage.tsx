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
import * as React from 'react';
import { useEffect, useState, useCallback } from 'react';
import styled from 'styled-components';

import { Row, Col, Button } from 'components/bootstrap';
import { DocumentTitle, Icon, PageHeader, Spinner, Timestamp } from 'components/common';
import { ClusterOverviewStore } from 'stores/cluster/ClusterOverviewStore';
import type { NodesStoreState } from 'stores/nodes/NodesStore';
import { NodesStore } from 'stores/nodes/NodesStore';
import Routes from 'routing/Routes';
import { useStore } from 'stores/connect';
import useParams from 'routing/useParams';
import useHistory from 'routing/useHistory';

function nodeFilter(state: NodesStoreState, nodeId: string) {
  return state.nodes && state.nodes[nodeId];
}

const StyledButton = styled(Button)`
  margin-left: 5px;
`;
const DEFAULT_LIMIT = 5000;

const SystemLogsPage = () => {
  const [isReloadingResults, setIsReloadingResults] = useState(false);
  const [sysLogs, setSysLogs] = useState(undefined);
  const [taken, setTaken] = useState<Date>(undefined);
  const { nodeId } = useParams();
  const history = useHistory();
  const node = useStore(NodesStore, (state: NodesStoreState) => nodeFilter(state, nodeId));

  const fetchLogs = useCallback(() => {
    setIsReloadingResults(true);

    ClusterOverviewStore.systemLogs(nodeId, DEFAULT_LIMIT).then((logs) => {
      setSysLogs(logs);
      setTaken(new Date());
    });

    setIsReloadingResults(false);
  }, [nodeId]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const onCancel = () => {
    history.push(Routes.SYSTEM.NODES.LIST);
  };

  const _isLoading = () => !node;

  if (_isLoading()) {
    return <Spinner />;
  }

  const { short_node_id: shortNodeId, hostname } = node;
  const title = (
    <span>
      The most recent system logs (limited to {DEFAULT_LIMIT}) of node {shortNodeId} / {hostname}
      &nbsp;
      <small>Taken at <Timestamp dateTime={taken} /></small>
    </span>
  );

  const control = (
    <Col md={12}>
      <div className="pull-left">
        <Button onClick={fetchLogs} disabled={isReloadingResults}>
          <small>Reload&nbsp;</small>
          <Icon name="sync" spin={isReloadingResults} />
        </Button>
        <StyledButton onClick={onCancel}>
          Back
        </StyledButton>
      </div>
    </Col>
  );

  const logs = sysLogs ? <pre className="threaddump">{sysLogs}</pre> : <Spinner />;

  return (
    <DocumentTitle title={`System Logs of node ${shortNodeId} / ${hostname}`}>
      <div>
        <PageHeader title={title} />
        <Row className="content">
          {control}
        </Row>
        <Row className="content">
          <Col md={12}>
            {logs}
          </Col>
        </Row>
        <Row className="content">
          {control}
        </Row>
      </div>
    </DocumentTitle>
  );
};

export default SystemLogsPage;
