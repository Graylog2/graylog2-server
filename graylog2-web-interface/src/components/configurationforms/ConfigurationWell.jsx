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

import { Well } from 'components/graylog';

class ConfigurationWell extends React.Component {
  PASSWORD_PLACEHOLDER = '********';

  static propTypes = {
    id: PropTypes.string.isRequired,
    configuration: PropTypes.any,
    typeDefinition: PropTypes.object,
  };

  static defaultProps = {
    configuration: undefined,
    typeDefinition: {},
  }

  _formatRegularField = (value, key) => {
    const { id } = this.props;
    let finalValue;

    if (value === null || value === undefined || value === '' || (Array.isArray(value) && value.length === 0)) {
      finalValue = <i>{'<empty>'}</i>;
    } else {
      finalValue = Array.isArray(value) ? value.join(', ') : String(value);
    }

    return (<li key={`${id}-${key}`}><div className="key">{key}:</div> <div className="value">{finalValue}</div></li>);
  };

  _formatPasswordField = (value, key) => {
    const { id } = this.props;

    return (
      <li key={`${id}-${key}`}>
        <div className="key">{key}:</div>
        <div className="value">{this.PASSWORD_PLACEHOLDER}</div>
      </li>
    );
  };

  _formatConfiguration = (id, config, typeDefinition) => {
    if (!config) {
      return ('');
    }

    const formattedItems = Object.keys(config).sort().map((key) => {
      const value = config[key];
      const requestedConfiguration = (typeDefinition && typeDefinition.requested_configuration ? typeDefinition.requested_configuration[key] : undefined);

      if (requestedConfiguration && requestedConfiguration.attributes.indexOf('is_password') > -1) {
        return this._formatPasswordField(value, key);
      }

      return this._formatRegularField(value, key);
    });

    if (formattedItems.length < 1) {
      formattedItems.push(<li key="placeholder">-- no configuration --</li>);
    }

    return (
      <ul>
        {formattedItems}
      </ul>
    );
  };

  render() {
    const { id, configuration, typeDefinition } = this.props;

    return (
      <Well bsSize="small" className="configuration-well react-configuration-well">
        {this._formatConfiguration(id, configuration, typeDefinition)}
      </Well>
    );
  }
}

export default ConfigurationWell;
