/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
                         values={values}
                         titleValue={titleValue}
                         submitAction={this._onSubmit}>
        <NodeOrGlobalSelect onChange={this._handleChange} global={this.state.global} node={this.state.node} />
      </ConfigurationForm>
    );
  }
}

export default InputForm;
