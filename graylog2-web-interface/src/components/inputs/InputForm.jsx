import PropTypes from 'prop-types';
import React from 'react';
import jQuery from 'jquery';

import { NodeOrGlobalSelect } from 'components/inputs';
import { ConfigurationForm } from 'components/configurationforms';

class InputForm extends React.Component {
  static propTypes = {
    globalValue: PropTypes.bool,
    nodeValue: PropTypes.string,
    titleValue: PropTypes.string,
    submitAction: PropTypes.func.isRequired,
    values: PropTypes.object,
  };

  state = {
    global: this.props.globalValue !== undefined ? this.props.globalValue : false,
    node: this.props.nodeValue !== undefined ? this.props.nodeValue : undefined,
  };

  _handleChange = (field, value) => {
    const state = {};
    state[field] = value;
    this.setState(state);
  };

  _onSubmit = (data) => {
    const newData = jQuery.extend(data, { global: this.state.global, node: this.state.node });
    this.props.submitAction(newData);
  };

  open = () => {
    this.configurationForm.open();
  };

  render() {
    const values = this.props.values ? this.props.values
      : (this.configurationForm ? this.configurationForm.getValue().configuration : {});
    const titleValue = this.props.titleValue ? this.props.titleValue
      : (this.configurationForm ? this.configurationForm.getValue().titleValue : '');
    return (
      <ConfigurationForm {...this.props}
                         ref={(configurationForm) => { this.configurationForm = configurationForm; }}
                         isInputForm
                         values={values}
                         titleValue={titleValue}
                         submitAction={this._onSubmit}>
        <NodeOrGlobalSelect onChange={this._handleChange} global={this.state.global} node={this.state.node} />
      </ConfigurationForm>
    );
  }
}

export default InputForm;
