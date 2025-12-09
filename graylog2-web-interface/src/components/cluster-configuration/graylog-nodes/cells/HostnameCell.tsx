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

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

import type { ClusterGraylogNode } from '../fetchClusterGraylogNodes';
import { NodePrimary, SecondaryText, StyledLabel } from '../../shared-components/NodeMetricsLayout';

type Props = {
  node: ClusterGraylogNode;
};

const getNodeDisplayName = (node: ClusterGraylogNode) => {
  const nodeNameParts = [node.short_node_id, node.hostname].filter(Boolean);

  if (nodeNameParts.length) {
    return nodeNameParts.join(' / ');
  }

  return node.node_id ?? node.hostname ?? node.id;
};

const HostnameCell = ({ node }: Props) => {
  const nodeId = node.node_id;
  const nodeName = getNodeDisplayName(node);

  return (
    <NodePrimary>
      {nodeId ? <Link to={Routes.SYSTEM.CLUSTER.NODE_SHOW(nodeId)}>{nodeName}</Link> : nodeName}
      {node.is_leader && (
        <SecondaryText>
          <StyledLabel bsSize="xs">Leader</StyledLabel>
        </SecondaryText>
      )}
    </NodePrimary>
  );
};

export default HostnameCell;
