import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Row, Col } from 'components/graylog';
import { Spinner, EntityList, Pluralize } from 'components/common';
import StoreProvider from 'injection/StoreProvider';

import NodeListItem from './NodeListItem';

const ClusterOverviewStore = StoreProvider.getStore('ClusterOverview');

const NodesList = createReactClass({
  displayName: 'NodesList',

  propTypes: {
    permissions: PropTypes.array.isRequired,
    nodes: PropTypes.object,
  },

  mixins: [Reflux.connect(ClusterOverviewStore)],

  _isLoading() {
    const { nodes } = this.props;
    const { clusterOverview } = this.state;

    return !(nodes && clusterOverview);
  },

  _formatNodes(nodes, clusterOverview) {
    const nodeIDs = Object.keys(nodes);

    return nodeIDs.map((nodeID) => {
      return <NodeListItem key={nodeID} node={nodes[nodeID]} systemOverview={clusterOverview[nodeID]} />;
    });
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const nodesNo = Object.keys(this.props.nodes).length;

    return (
      <Row className="content">
        <Col md={12}>
          <h2>
            There <Pluralize value={nodesNo} singular="is" plural="are" /> {nodesNo} active <Pluralize value={nodesNo} singular="node" plural="nodes" />
          </h2>
          <EntityList bsNoItemsStyle="info"
                      noItemsText="There are no active nodes."
                      items={this._formatNodes(this.props.nodes, this.state.clusterOverview)} />
        </Col>
      </Row>
    );
  },
});

export default NodesList;
