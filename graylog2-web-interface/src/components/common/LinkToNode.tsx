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

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import AppConfig from 'util/AppConfig';
import HideOnCloud from 'util/conditional/HideOnCloud';
import { NodesStore } from 'stores/nodes/NodesStore';
import connect from 'stores/connect';

import Icon from './Icon';
import Spinner from './Spinner';

type LinkToNodeProps = {
  /** Node ID that will be used to generate the link. */
  nodeId: string;
  nodes?: any;
};

/**
 * Component that creates a link to a Graylog node. The information in the link includes:
 *  - Marker indicating whether the Graylog node is leader or not
 *  - Short Graylog node ID
 *  - Graylog node hostname
 *
 * All this information will be obtained from the `NodesStore`.
 */

class LinkToNode extends React.PureComponent<LinkToNodeProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    nodes: undefined,
  };

  render() {
    const { nodes } = this.props;

    if (!nodes) {
      return <Spinner />;
    }

    const node = nodes[this.props.nodeId];

    if (node) {
      const iconName = node.is_leader ? 'star' : 'fork_right';
      const iconClass = node.is_leader ? 'leader-node' : '';
      const iconTitle = node.is_leader ? 'This is the leader node in the cluster' : '';

      const content = (
        <>
          <Icon name={iconName} className={iconClass} title={iconTitle} />
          {' '}
          {node.short_node_id}<HideOnCloud> / {node.hostname}</HideOnCloud>
        </>
      );

      if (AppConfig.isCloud()) {
        return content;
      }

      return (
        <Link to={Routes.SYSTEM.NODES.SHOW(this.props.nodeId)}>{content}</Link>
      );
    }

    return <i>Unknown Node</i>;
  }
}

export default connect(
  LinkToNode,
  { nodeStore: NodesStore },
  ({ nodeStore, ...rest }) => ({ ...rest, nodes: nodeStore.nodes }),
);
