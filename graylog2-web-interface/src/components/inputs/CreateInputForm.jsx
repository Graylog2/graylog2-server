import React from 'react';
import Reflux from 'reflux';
import jQuery from 'jquery';

import NodesStore from 'stores/nodes/NodesStore';

import { Spinner } from 'components/common';
import { BooleanField, ConfigurationForm, DropdownField } from 'components/configurationforms';

const CreateInputForm = React.createClass({
  mixins: [Reflux.connect(NodesStore)],
  getInitialState() {
    return {
      global: false
    };
  },
  _handleChange(field, value) {
    const state = {};
    state[field] = value;
    this.setState(state);
  },
  _onSubmit(data) {
    const newData = jQuery.extend(data, {global: this.state.global, node: this.state.node});
    this.props.submitAction(newData);
  },
  open() {
    this.refs.configurationForm.open();
  },
  render() {
    if (!this.state.nodes) {
      return <Spinner />;
    }
    const formattedNodes = {};
    Object.keys(this.state.nodes).forEach(nodeId => formattedNodes[nodeId] = this.state.nodes[nodeId].short_node_id);
    const nodeSelectField = {
      human_name: 'Node',
      description: 'On which node should this input start',
      'additional_info': {
        values: formattedNodes,
      },
    };
    const additionalFields = [
      <BooleanField key="input-global" typeName="input" title="global" value={this.state.global}
                    field={{human_name: 'Global', description: 'Should this input start on all nodes', default_value: false}}
                    onChange={this._handleChange} autoFocus />,
      <DropdownField key="input-node" typeName="input" title="node" value={this.state.node}
                     field={nodeSelectField} disabled={this.state.global}
                     onChange={this._handleChange} autoFocus={false} />
    ];
    const values = this.refs.configurationForm ? this.refs.configurationForm.getValue() : {title: '', configuration: {}};
    return (
      <ConfigurationForm {...this.props} ref="configurationForm" values={values.configuration} titleValue={values.title}
                         additionalFields={additionalFields}
                         submitAction={this._onSubmit} />
    );
  },
});

export default CreateInputForm;
