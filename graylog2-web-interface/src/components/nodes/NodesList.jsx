import React, {PropTypes} from 'react';
import Reflux from 'reflux';
import { Row, Col } from 'react-bootstrap';

import NodeListItem from './NodeListItem';
import { Spinner, EntityList, Pluralize } from 'components/common';

import NodesStore from 'stores/nodes/NodesStore';
import ClusterOverviewStore from 'stores/cluster/ClusterOverviewStore';

const NodesList = React.createClass({
  propTypes: {
    permissions: PropTypes.array.isRequired,
  },
  mixins: [Reflux.connect(NodesStore), Reflux.connect(ClusterOverviewStore)],
  _isLoading() {
    return !(this.state.nodes && this.state.clusterOverview);
  },
  _formatNodes(nodes, clusterOverview) {
    const nodeIDs = Object.keys(nodes);

    return nodeIDs.map(nodeID => {
      return <NodeListItem key={nodeID} node={nodes[nodeID]} systemOverview={clusterOverview[nodeID]}/>;
    });
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    const nodesNo = Object.keys(this.state.nodes).length;

    return (
      <Row className="content">
        <Col md={12}>
          <h2>
            There <Pluralize value={nodesNo} singular="is" plural="are"/> {nodesNo} active <Pluralize value={nodesNo} singular="node" plural="nodes"/>
          </h2>
          <EntityList bsNoItemsStyle="info" noItemsText="There are no active nodes."
                      items={this._formatNodes(this.state.nodes, this.state.clusterOverview)} />
        </Col>
      </Row>
    );
  },
});

export default NodesList;
