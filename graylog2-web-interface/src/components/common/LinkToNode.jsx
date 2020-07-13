import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { Link } from 'react-router';

import StoreProvider from 'injection/StoreProvider';
import Routes from 'routing/Routes';

import Icon from './Icon';
import Spinner from './Spinner';

const NodesStore = StoreProvider.getStore('Nodes');

/**
 * Component that creates a link to a Graylog node. The information in the link includes:
 *  - Marker indicating whether the Graylog node is master or not
 *  - Short Graylog node ID
 *  - Graylog node hostname
 *
 * All this information will be obtained from the `NodesStore`.
 */
const LinkToNode = createReactClass({
  displayName: 'LinkToNode',

  propTypes: {
    /** Node ID that will be used to generate the link. */
    nodeId: PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(NodesStore)],

  render() {
    if (!this.state.nodes) {
      return <Spinner />;
    }

    const node = this.state.nodes[this.props.nodeId];

    if (node) {
      const iconName = node.is_master ? 'star' : 'code-branch';
      const iconClass = node.is_master ? 'master-node' : '';
      const iconTitle = node.is_master ? 'This is the master node in the cluster' : '';

      return (
        <Link to={Routes.SYSTEM.NODES.SHOW(this.props.nodeId)}>
          <Icon name={iconName} className={iconClass} title={iconTitle} />
          {' '}
          {node.short_node_id} / {node.hostname}
        </Link>
      );
    }

    return <i>Unknown Node</i>;
  },
});

export default LinkToNode;
