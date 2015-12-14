import React from 'react';
import Reflux from 'reflux';
import { Link } from 'react-router';

import NodesStore from 'stores/nodes/NodesStore';

import Routes from 'routing/Routes';
import { Spinner } from 'components/common';

const LinkToNode = React.createClass({
  mixins: [Reflux.connect(NodesStore)],
  propTypes: {
    nodeId: React.PropTypes.string.isRequired,
  },
  render() {
    if (!this.state.nodes) {
      return <Spinner />;
    }
    const node = this.state.nodes[this.props.nodeId];
    if (node) {
      return (
        <Link to={Routes.SYSTEM.NODES.SHOW(this.props.nodeId)}>
          <i class="fa fa-code-fork"/>
          {node.short_node_id} / {node.hostname}
        </Link>
      );
    }
    return <i>Unknown Node</i>;
  },
});

export default LinkToNode;
