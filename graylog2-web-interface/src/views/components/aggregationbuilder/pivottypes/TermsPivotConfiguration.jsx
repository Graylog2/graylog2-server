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
import React from 'react';
import PropTypes from 'prop-types';

import * as FormsUtils from 'util/FormsUtils';
import Input from 'components/bootstrap/Input';

export default class TermsPivotConfiguration extends React.Component {
  static propTypes = {
    onChange: PropTypes.func.isRequired,
    value: PropTypes.shape({
      limit: PropTypes.number.isRequired,
    }).isRequired,
  };

  constructor(props, context) {
    super(props, context);

    const { limit } = props.value;

    this.state = { limit };
  }

  _changeLimit = (event) => {
    const input = FormsUtils.getValueFromInput(event.target);
    const limit = Math.max(input, 1);

    this._propagateChange(limit);
  };

  _propagateChange = (limit) => this.setState((state) => {
    const newState = { limit };

    if (state.limit && !isNaN(state.limit)) {
      newState.oldLimit = state.limit;
    }

    return newState;
  }, () => this.props.onChange({ limit }));

  _onBlur = () => {
    if (!this.state.limit && this.state.oldLimit) {
      this._propagateChange(this.state.oldLimit);
    }
  };

  render() {
    return (
      <Input type="number"
             id="stage"
             name="stage"
             label="Number of Values"
             autoFocus
             onBlur={this._onBlur}
             onChange={this._changeLimit}
             help="The number of values returned for this field"
             value={this.state.limit} />
    );
  }
}
