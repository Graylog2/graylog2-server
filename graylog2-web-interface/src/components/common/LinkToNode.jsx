import React from 'react';
import Reflux from 'reflux';
import { Link } from 'react-router';

import NodesStore from 'stores/nodes/NodesStore';

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

    let icon;
    if (node.is_master) {
      icon = <i className="fa fa-star master-node"/>;
    } else {
      icon = <i className="fa fa-code-fork"/>;
    }

    if (node) {
      return (
        <Link to={Routes.SYSTEM.NODES.SHOW(this.props.nodeId)}>
          {icon}
          {' '}
          {node.short_node_id} / {node.hostname}
        </Link>
      );
    }
    return <i>Unknown Node</i>;
  },
});

export default LinkToNode;
