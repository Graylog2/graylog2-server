import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import NodesStore from 'stores/nodes/NodesStore';
import ClusterOverviewStore from 'stores/cluster/ClusterOverviewStore';
import PluginsStore from 'stores/plugins/PluginsStore';
import InputStatesStore from 'stores/inputs/InputStatesStore';
import InputTypesStore from 'stores/inputs/InputTypesStore';

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
  mixins: [
    Reflux.connectFilter(NodesStore, 'node', nodeFilter),
    Reflux.connectFilter(ClusterOverviewStore, 'systemOverview', clusterOverviewFilter),
    Reflux.connect(InputTypesStore),
  ],
  getInitialState() {
    return {
      jvmInformation: undefined,
      plugins: undefined,
    };
  },
  componentWillMount() {
    ClusterOverviewStore.jvm(this.props.params.nodeId)
      .then(jvmInformation => this.setState({jvmInformation: jvmInformation}));
    PluginsStore.list(this.props.params.nodeId).then(plugins => this.setState({plugins: plugins}));
    InputStatesStore.list().then(inputStates => {
      // We only want the input states for the current node
      const inputIds = Object.keys(inputStates);
      const filteredInputStates = [];
      inputIds.forEach(inputId => {
        const inputObject = inputStates[inputId][this.props.params.nodeId];
        if (inputObject) {
          filteredInputStates.push(inputObject);
        }
      });

      this.setState({inputStates: filteredInputStates});
    });
  },
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
            {node.is_master ? <span>This is the master node.</span> : <span>This is <em>not</em> the master node.</span>}
          </span>
          <span><NodeMaintenanceDropdown node={node}/></span>
        </PageHeader>
        <NodeOverview node={node} systemOverview={this.state.systemOverview}
                      jvmInformation={this.state.jvmInformation} plugins={this.state.plugins}
                      inputStates={this.state.inputStates} inputDescriptions={this.state.inputDescriptions}/>
      </div>
    );
  },
});

export default ShowNodePage;
