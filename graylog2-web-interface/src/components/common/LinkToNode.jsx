import React from 'react';
import Reflux from 'reflux';
import { Link } from 'react-router';

import StoreProvider from 'injection/StoreProvider';
const NodesStore = StoreProvider.getStore('Nodes');

import Routes from 'routing/Routes';
import { Spinner } from 'components/common';

const LinkToNode = React.createClass({
  propTypes: {
    nodeId: React.PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(NodesStore)],
  render() {
    if (!this.state.nodes) {
      return <Spinner />;
    }
    const node = this.state.nodes[this.props.nodeId];

    if (node) {
      const iconClass = node.is_master ? 'fa fa-star master-node' : 'fa fa-code-fork';
      const iconTitle = node.is_master ? 'This is the master node in the cluster' : '';
      return (
        <Link to={Routes.SYSTEM.NODES.SHOW(this.props.nodeId)}>
          <i className={iconClass} title={iconTitle} />
          {' '}
          {node.short_node_id} / {node.hostname}
        </Link>
      );
    }
    return <i>Unknown Node</i>;
  },
});

export default LinkToNode;
