import React from 'react';
import Reflux from 'reflux';
import { Input } from 'components/bootstrap';

import StoreProvider from 'injection/StoreProvider';
const NodesStore = StoreProvider.getStore('Nodes');

import { Spinner } from 'components/common';

const NodeOrGlobalSelect = React.createClass({
  propTypes: {
    global: React.PropTypes.bool,
    onChange: React.PropTypes.func.isRequired,
    node: React.PropTypes.string,
  },
  mixins: [Reflux.connect(NodesStore)],
  getInitialState() {
    return {
      global: this.props.global !== undefined ? this.props.global : false,
      node: this.props.node,
    };
  },
  _onChangeGlobal(evt) {
    const global = evt.target.checked;
    this.setState({ global: global });
    if (global) {
      this.setState({ node: 'placeholder' });
      this.props.onChange('node', undefined);
    } else {
      this.props.onChange('node', this.state.node);
    }
    this.props.onChange('global', global);
  },
  _onChangeNode(evt) {
    this.setState({ node: evt.target.value });
    this.props.onChange('node', evt.target.value);
  },
  render() {
    if (!this.state.nodes) {
      return <Spinner />;
    }

    const options = Object.keys(this.state.nodes)
      .map((nodeId) => {
        return <option key={nodeId} value={nodeId}>{this.state.nodes[nodeId].short_node_id} / {this.state.nodes[nodeId].hostname}</option>;
      });

    const nodeSelect = !this.state.global ? (
      <Input type="select" label="Node" placeholder="placeholder" value={this.state.node}
             help="On which node should this input start" onChange={this._onChangeNode} required>
        <option key="placeholder" value="">Select Node</option>
        {options}
      </Input>
    ) : null;

    return (
      <span>
        <Input type="checkbox" label="Global" help="Should this input start on all nodes"
               checked={this.state.global} onChange={this._onChangeGlobal} />
        {nodeSelect}
      </span>
    );
  },
});

export default NodeOrGlobalSelect;
