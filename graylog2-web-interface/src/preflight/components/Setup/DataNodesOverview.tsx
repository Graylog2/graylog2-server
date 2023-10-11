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
import { Space } from '@mantine/core';
import styled from 'styled-components';

import Spinner from 'components/common/Spinner';
import useDataNodes from 'preflight/hooks/useDataNodes';
import { Alert, Badge, List } from 'preflight/components/common';
import Icon from 'preflight/components/common/Icon';
import type { DataNodeStatus } from 'preflight/types';

const P = styled.p`
  max-width: 700px;
`;

const NodeId = styled(Badge)`
  margin-right: 3px;
`;

const SecureIcon = styled(Icon)`
  margin-right: 3px;
`;

type NodeProps = {
  status: DataNodeStatus,
  nodeId: string,
  transportAddress: string,
};

const isSecure = (address: string) => address?.toLocaleLowerCase().startsWith('https://');

const colorByState = (status: DataNodeStatus, address: string) => {
  if (status === 'CONNECTING') {
    return 'yellow';
  }

  if (status === 'ERROR') {
    return 'red';
  }

  if (!address) {
    return 'grey';
  }

  return isSecure(address) ? 'green' : 'red';
};

const lockIcon = (address: string) => (isSecure(address) ? 'lock' : 'unlock');
const isConnecting = (status: DataNodeStatus) => status === 'CONNECTING';
const ConnectingSpinner = () => <Spinner text="" />;

const Node = ({ nodeId, transportAddress, status }: NodeProps) => (
  <NodeId color={colorByState(status, transportAddress)} title="Short node id">
    <SecureIcon name={lockIcon(transportAddress)} />{nodeId}
    {isConnecting(status) ? <>{' '}<ConnectingSpinner /></> : null}
  </NodeId>
);

const ErrorBadge = styled(Badge)`
  margin-left: 5px;
`;
const Error = ({ message }: { message: string }) => <ErrorBadge title={message} color="red">{message}</ErrorBadge>;

const DataNodesOverview = () => {
  const {
    data: dataNodes,
    isFetching: isFetchingDataNodes,
    isInitialLoading: isInitialLoadingDataNodes,
  } = useDataNodes();

  return (
    <>
      <P>
        Graylog data nodes offer a better integration with Graylog and simplify future updates.
        Once a Graylog data node is running and you configured the certificate authority, you can resume startup.
      </P>
      <P>
        These are the data nodes which are currently registered.
        The list is constantly updated. {isFetchingDataNodes && <Spinner text="" />}
      </P>

      {!!dataNodes.length && (
        <>
          <Space h="sm" />
          <List spacing="xs">
            {dataNodes.map(({
              hostname,
              transport_address,
              short_node_id,
              status,
              error_msg,

            }) => (
              <List.Item key={short_node_id}>
                <Node status={status} nodeId={short_node_id} transportAddress={transport_address} />
                <span title="Transport address">{transport_address}</span>{' – '}
                <span title="Hostname">{hostname}</span>
                {error_msg && <Error message={error_msg} />}
              </List.Item>
            ))}
          </List>
        </>
      )}
      {(!dataNodes.length && !isInitialLoadingDataNodes) && (
        <Alert type="info">
          No data nodes have been found.
        </Alert>
      )}
      <Space h="md" />
    </>
  );
};

export default DataNodesOverview;
