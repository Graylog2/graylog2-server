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
import $ from 'jquery';
import PropTypes from 'prop-types';
import React from 'react';

import FieldHelpers from 'components/configurationforms/FieldHelpers';

class DropdownField extends React.Component {
  static propTypes = {
    autoFocus: PropTypes.bool.isRequired,
    field: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    typeName: PropTypes.string.isRequired,
    value: PropTypes.any,
    addPlaceholder: PropTypes.bool,
    disabled: PropTypes.bool,
  };

  static defaultProps = {
    addPlaceholder: false,
  };

  state = {
    typeName: this.props.typeName,
    field: this.props.field,
    title: this.props.title,
    value: this.props.value,
  };

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillReceiveProps(props) {
    this.setState(props);
  }

  _formatOption = (value, key, disabled) => {
    return (
      <option key={`${this.state.typeName}-${this.state.title}-${key}`} value={key} id={key} disabled={disabled}>{value}</option>
    );
  };

  handleChange = (evt) => {
    this.props.onChange(this.state.title, evt.target.value);
    this.setState({ value: evt.target.value });
  };

  render() {
    const { field, title, typeName } = this.state;
    const fieldId = `${typeName}-${title}`;

    const options = $.map(field.additional_info.values, this._formatOption);

    if (this.props.addPlaceholder) {
      options.unshift(this._formatOption(`Select ${field.human_name || this.state.title}`, '', true));
    }

    return (
      <div className="form-group">
        <label htmlFor={fieldId}>
          {field.human_name}

          {FieldHelpers.optionalMarker(field)}
        </label>

        <select id={fieldId}
                value={this.state.value}
                className="input-xlarge form-control"
                onChange={this.handleChange}
                autoFocus={this.props.autoFocus}
                disabled={this.props.disabled}
                required={!field.is_optional}>
          {options}
        </select>
        <p className="help-block">{field.description}</p>
      </div>
    );
  }
}

export default DropdownField;
