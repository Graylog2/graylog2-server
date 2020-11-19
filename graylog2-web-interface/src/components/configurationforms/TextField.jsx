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

import FieldHelpers from 'components/configurationforms/FieldHelpers';

class TextField extends React.Component {
  static propTypes = {
    autoFocus: PropTypes.bool,
    field: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    typeName: PropTypes.string.isRequired,
    value: PropTypes.any,
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

  handleChange = (evt) => {
    this.props.onChange(this.state.title, evt.target.value);
    this.setState({ value: evt.target.value });
  };

  render() {
    const { field } = this.state;
    const { title } = this.state;
    const { typeName } = this.state;

    let inputField;
    const isRequired = !field.is_optional;
    const fieldType = (!FieldHelpers.hasAttribute(field.attributes, 'textarea') && FieldHelpers.hasAttribute(field.attributes, 'is_password') ? 'password' : 'text');

    if (FieldHelpers.hasAttribute(field.attributes, 'textarea')) {
      inputField = (
        <textarea id={title}
                  className="form-control"
                  rows={10}
                  name={`configuration[${title}]`}
                  required={isRequired}
                  value={this.state.value}
                  onChange={this.handleChange}
                  autoFocus={this.props.autoFocus} />
      );
    } else {
      inputField = (
        <input id={title}
               type={fieldType}
               className="form-control"
               name={`configuration[${title}]`}
               value={this.state.value}
               onChange={this.handleChange}
               required={isRequired}
               autoFocus={this.props.autoFocus} />
      );
    }

    // TODO: replace with bootstrap input component
    return (
      <div className="form-group">
        <label htmlFor={`${typeName}-${title})`}>
          {field.human_name}
          {FieldHelpers.optionalMarker(field)}
        </label>
        {inputField}
        <p className="help-block">{field.description}</p>
      </div>
    );
  }
}

export default TextField;
