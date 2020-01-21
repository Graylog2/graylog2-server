// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import Routes from 'routing/Routes';
import { Icon } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';
import connect from 'stores/connect';

const { NodesStore } = CombinedProvider.get('Nodes');

type NodeId = string;
type NodeInfo = {
  short_node_id: string,
  hostname: string,
}
type Props = {
  nodeId: NodeId,
  nodes: { [NodeId]: NodeInfo },
};

const NodeName = ({ nodeId, nodes }: Props) => {
  const node = nodes[nodeId];
  if (node) {
    const nodeURL = Routes.node(nodeId);
    return (
      <a href={nodeURL}>
        <Icon name="code-fork" />
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

export default connect(NodeName, { nodes: NodesStore }, ({ nodes: { nodes = {} } = {} }) => ({ nodes }));
