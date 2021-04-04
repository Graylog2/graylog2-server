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

class RadioField extends React.Component {
  static propTypes = {
    autoFocus: PropTypes.bool.isRequired,
    field: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    typeName: PropTypes.string.isRequired,
    value: PropTypes.any,
    disabled: PropTypes.bool,
  };

  static defaultProps = {};

  state = {
    typeName: this.props.typeName,
    field: this.props.field,
    title: this.props.title,
    value: this.props.value,
  };

  componentWillReceiveProps(props) {
    this.setState(props);
  }

  handleChange = (evt) => {
    this.props.onChange(this.props.title, evt.target.value);
    this.setState({ value: evt.target.value });
  };

  render() {
    const { field } = this.state;
    const options = [];
    const { typeName } = this.state;

    Object.entries(field.additional_info.values).map(([key, value]) => {
       options.push(
         <div className="radio" key={`${typeName}-${field.title}-${key}`}>
           <label>
             <input className="input-xlarge"
                    type="radio"
                    name={key}
                    value={value}
                    id={key}
                    onChange={this.handleChange}
                    autoFocus={this.state.autoFocus}
                    checked={this.state.value == key} />
           {value}</label>
          </div>
       )
     });

    return (
      <div className="form-group">
        <label htmlFor={`${typeName}-${field.title}`}>
          {field.human_name}
          {FieldHelpers.optionalMarker(field)}
        </label>
        {options}
        <p className="help-block">{field.description}</p>
      </div>
    );
  }
}

export default RadioField;
