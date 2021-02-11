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

import * as FormsUtils from 'util/FormsUtils';

import FieldHelpers from './FieldHelpers';

class BooleanField extends React.Component {
  static propTypes = {
    autoFocus: PropTypes.bool,
    field: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    title: PropTypes.string.isRequired,
    typeName: PropTypes.string.isRequired,
    value: PropTypes.any,
  };

  render() {
    const { field } = this.props;
    const { typeName } = this.props;
    const { title } = this.props;

    return (
      <div className="form-group">
        <div className="checkbox">
          <label>
            <input id={`${typeName}-${title}`}
                   type="checkbox"
                   checked={this.props.value}
                   name={`configuration[${title}]`}
                   onChange={this.handleChange} />

            {field.human_name}

            {FieldHelpers.optionalMarker(field)}
          </label>
        </div>
        <p className="help-block">{field.description}</p>
      </div>
    );
  }

  handleChange = (event) => {
    const newValue = FormsUtils.getValueFromInput(event.target);

    this.props.onChange(this.props.title, newValue);
  };
}

export default BooleanField;
