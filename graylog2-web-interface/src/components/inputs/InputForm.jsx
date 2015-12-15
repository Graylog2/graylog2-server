import React from 'react';
import Reflux from 'reflux';
import jQuery from 'jquery';

import NodesStore from 'stores/nodes/NodesStore';

import { Spinner } from 'components/common';
import { NodeOrGlobalSelect } from 'components/inputs';
import { BooleanField, ConfigurationForm, DropdownField } from 'components/configurationforms';

const InputForm = React.createClass({
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
    const values = this.refs.configurationForm ? this.refs.configurationForm.getValue() : {title: '', configuration: {}};
    return (
      <ConfigurationForm {...this.props} ref="configurationForm" values={values.configuration} titleValue={values.title}
                         submitAction={this._onSubmit}>
        <NodeOrGlobalSelect onChange={this._handleChange} />
      </ConfigurationForm>
    );
  },
});

export default InputForm;
