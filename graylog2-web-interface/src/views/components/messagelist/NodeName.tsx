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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import Routes from 'routing/Routes';
import { Icon } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';
import { NodesStoreState } from 'stores/nodes/NodesStore';
import { Store } from 'stores/StoreTypes';

const { NodesStore } = CombinedProvider.get('Nodes');

type NodeId = string;
type NodeInfo = {
  short_node_id: string,
  hostname: string,
};
type Props = {
  nodeId: NodeId,
  nodes: { [key: string]: NodeInfo },
};

const NodeName = ({ nodeId, nodes }: Props) => {
  const node = nodes[nodeId];

  if (node) {
    const nodeURL = Routes.node(nodeId);

    return (
      <a href={nodeURL}>
        <Icon name="code-branch" />
        &nbsp;
        <span style={{ wordBreak: 'break-word' }}>
          {node.short_node_id}
        </span>&nbsp;/&nbsp;
        <span style={{ wordBreak: 'break-word' }}>
          {node.hostname}
        </span>
      </a>
    );
  }

  return <span style={{ wordBreak: 'break-word' }}>stopped node</span>;
};

NodeName.propTypes = {
  nodeId: PropTypes.string.isRequired,
};

export default connect(NodeName, { nodes: NodesStore as Store<NodesStoreState> }, ({ nodes: { nodes = {} } = {} }) => ({ nodes }));
