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

import ObjectUtils from 'util/ObjectUtils';
import { Input } from 'components/bootstrap';
import { Select, TimeUnitInput } from 'components/common';

class MaxmindAdapterFieldSet extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
    handleFormEvent: PropTypes.func.isRequired,
    validationState: PropTypes.func.isRequired,
    validationMessage: PropTypes.func.isRequired,
  };

  _update = (value, unit, enabled, name) => {
    const config = ObjectUtils.clone(this.props.config);

    config[name] = enabled ? value : 0;
    config[`${name}_unit`] = unit;
    this.props.updateConfig(config);
  };

  updateCheckInterval = (value, unit, enabled) => {
    this._update(value, unit, enabled, 'check_interval');
  };

  _onDbTypeSelect = (id) => {
    const config = ObjectUtils.clone(this.props.config);

    config.database_type = id;
    this.props.updateConfig(config);
  };

  render() {
    const { config } = this.props;
    const databaseTypes = [
      { label: 'ASN database', value: 'MAXMIND_ASN' },
      { label: 'City database', value: 'MAXMIND_CITY' },
      { label: 'Country database', value: 'MAXMIND_COUNTRY' },
      { label: 'IPinfo location database', value: 'IPINFO_STANDARD_LOCATION' },
      { label: 'IPinfo ASN database', value: 'IPINFO_ASN' },
    ];

    return (
      <fieldset>
        <Input type="text"
               id="path"
               name="path"
               label="File path"
               autoFocus
               required
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('path', 'The path to the Maxmind\u2122 database file.')}
               bsStyle={this.props.validationState('path')}
               value={config.path}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input id="database-type-select"
               label="Database type"
               required
               autoFocus
               help="Select the type of the database file"
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9">
          <Select placeholder="Select the type of database file"
                  clearable={false}
                  options={databaseTypes}
                  matchProp="label"
                  onChange={this._onDbTypeSelect}
                  value={config.database_type} />
        </Input>
        <TimeUnitInput label="Refresh file"
                       help={'If enabled, the MaxMind\u2122 database file is checked for modifications and refreshed when it changed on disk.'}
                       update={this.updateCheckInterval}
                       value={config.check_interval}
                       unit={config.check_interval_unit || 'MINUTES'}
                       defaultEnabled={config.check_interval > 0}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9" />
      </fieldset>
    );
  }
}

export default MaxmindAdapterFieldSet;
