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

import { Button } from 'components/graylog';
import { Icon } from 'components/common';
import StoreProvider from 'injection/StoreProvider';

const InputStaticFieldsStore = StoreProvider.getStore('InputStaticFields');

class InputStaticFields extends React.Component {
  static propTypes = {
    input: PropTypes.object.isRequired,
  };

  _deleteStaticField = (fieldName) => {
    return () => {
      if (window.confirm(`Are you sure you want to remove static field '${fieldName}' from '${this.props.input.title}'?`)) {
        InputStaticFieldsStore.destroy(this.props.input, fieldName);
      }
    };
  };

  _deleteButton = (fieldName) => {
    return (
      <Button bsStyle="link" bsSize="xsmall" style={{ verticalAlign: 'baseline' }} onClick={this._deleteStaticField(fieldName)}>
        <Icon name="remove" />
      </Button>
    );
  };

  _formatStaticFields = (staticFields) => {
    const formattedFields = [];
    const staticFieldNames = Object.keys(staticFields);

    staticFieldNames.forEach((fieldName) => {
      formattedFields.push(
        <li key={`${fieldName}-field`}>
          <strong>{fieldName}:</strong> {staticFields[fieldName]} {this._deleteButton(fieldName)}
        </li>,
      );
    });

    return formattedFields;
  };

  render() {
    const staticFieldNames = Object.keys(this.props.input.static_fields);

    if (staticFieldNames.length === 0) {
      return <div />;
    }

    return (
      <div className="static-fields">
        <h3 style={{ marginBottom: 5 }}>Static fields</h3>
        <ul>
          {this._formatStaticFields(this.props.input.static_fields)}
        </ul>
      </div>
    );
  }
}

export default InputStaticFields;
