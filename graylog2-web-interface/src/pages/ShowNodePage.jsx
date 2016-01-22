import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import NodesStore from 'stores/nodes/NodesStore';
import ClusterOverviewStore from 'stores/cluster/ClusterOverviewStore';

import { NodeMaintenanceDropdown, NodeOverview } from 'components/nodes';
import { PageHeader, Spinner } from 'components/common';

function nodeFilter(state) {
  return state.nodes ? state.nodes[this.props.params.nodeId] : state.nodes;
}

function clusterOverviewFilter(state) {
  return state.clusterOverview ? state.clusterOverview[this.props.params.nodeId] : undefined;
}

const ShowNodePage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connectFilter(NodesStore, 'node', nodeFilter), Reflux.connectFilter(ClusterOverviewStore, 'systemOverview', clusterOverviewFilter)],
  _isLoading() {
    return !(this.state.node && this.state.systemOverview);
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }
    const node = this.state.node;
    const title = <span>Node {node.short_node_id} / {node.hostname}</span>;

    return (
      <div>
        <PageHeader title={title}>
          <span>
            This page shows details of a Graylog server node that is active and reachable in your cluster.
          </span>
          <span>
            {node.is_master && <span>This is the master node.</span>}
          </span>
          <span><NodeMaintenanceDropdown node={node}/></span>
        </PageHeader>
        <NodeOverview node={node} systemOverview={this.state.systemOverview}/>
      </div>
    );
  },
});

export default ShowNodePage;
