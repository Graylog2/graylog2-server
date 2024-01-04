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
import styled from 'styled-components';

import type { DataNodeStatus } from 'preflight/types';
import { Badge } from 'preflight/components/common';
import Icon from 'preflight/components/common/Icon';

import Spinner from '../../common/Spinner';

const NodeId = styled(Badge)`
  margin-right: 3px;
`;

const SecureIcon: React.ComponentType<{ name: 'lock' | 'unlock' }> = styled(Icon)`
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

const DataNodeBadge = ({ nodeId, transportAddress, status }: NodeProps) => (
  <NodeId color={colorByState(status, transportAddress)} title="Short node id">
    <SecureIcon name={lockIcon(transportAddress)} />{nodeId}
    {isConnecting(status) ? <>{' '}<ConnectingSpinner /></> : null}
  </NodeId>
);

export default DataNodeBadge;
