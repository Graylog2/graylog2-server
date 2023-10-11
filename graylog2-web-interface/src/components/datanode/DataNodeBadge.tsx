import * as React from 'react';
import styled from 'styled-components';

import type { DataNodeStatus } from 'preflight/types';
import { Badge } from 'preflight/components/common';
import Icon from 'preflight/components/common/Icon';

import Spinner from '../common/Spinner';

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

const DataNodeBadge = ({ nodeId, transportAddress, status }: NodeProps) => (
  <NodeId color={colorByState(status, transportAddress)} title="Short node id">
    <SecureIcon name={lockIcon(transportAddress)} />{nodeId}
    {isConnecting(status) ? <>{' '}<ConnectingSpinner /></> : null}
  </NodeId>
);

export default DataNodeBadge;
