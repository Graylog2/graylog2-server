import React from 'react';
import jQuery from 'jquery';

import { NodeOrGlobalSelect } from 'components/inputs';
import { ConfigurationForm } from 'components/configurationforms';

const InputForm = React.createClass({
  propTypes: {
    globalValue: React.PropTypes.bool,
    nodeValue: React.PropTypes.string,
    titleValue: React.PropTypes.string,
    submitAction: React.PropTypes.func.isRequired,
    values: React.PropTypes.object,
  },
  getInitialState() {
    return {
      global: this.props.globalValue !== undefined ? this.props.globalValue : false,
      node: this.props.nodeValue !== undefined ? this.props.nodeValue : undefined,
    };
  },
  _handleChange(field, value) {
    const state = {};
    state[field] = value;
    this.setState(state);
  },
  _onSubmit(data) {
    const newData = jQuery.extend(data, { global: this.state.global, node: this.state.node });
    this.props.submitAction(newData);
  },
  open() {
    this.refs.configurationForm.open();
  },
  render() {
    const values = this.props.values ? this.props.values :
      (this.refs.configurationForm ? this.refs.configurationForm.getValue().configuration : {});
    const titleValue = this.props.titleValue ? this.props.titleValue :
      (this.refs.configurationForm ? this.refs.configurationForm.getValue().titleValue : '');
    return (
      <ConfigurationForm {...this.props} ref="configurationForm" values={values} titleValue={titleValue}
                         submitAction={this._onSubmit}>
        <NodeOrGlobalSelect onChange={this._handleChange} global={this.state.global} node={this.state.node} />
      </ConfigurationForm>
    );
  },
});

export default InputForm;
