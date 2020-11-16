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
import moment from 'moment';

import { Input } from 'components/bootstrap';

class TimeBasedRotationStrategyConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    jsonSchema: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
  };

  state = {
    rotation_period: this.props.config.rotation_period,
  };

  inputs = {};

  _onPeriodUpdate = (field) => {
    return () => {
      const update = {};
      let period = this.inputs[field].getValue().toUpperCase();

      if (!period.startsWith('P')) {
        period = `P${period}`;
      }

      update[field] = period;

      this.setState(update);

      if (this._isValidPeriod(update[field])) {
        // Only propagate state if the config is valid.
        this.props.updateConfig(update);
      }
    };
  };

  _isValidPeriod = (duration) => {
    const check = duration || this.state.rotation_period;

    return moment.duration(check).asMilliseconds() >= 3600000;
  };

  _validationState = () => {
    if (this._isValidPeriod()) {
      return undefined;
    }

    return 'error';
  };

  _formatDuration = () => {
    return this._isValidPeriod() ? moment.duration(this.state.rotation_period).humanize() : 'invalid (min 1 hour)';
  };

  render() {
    return (
      <div>
        <Input id="rotation-period"
               type="text"
               ref={(rotationPeriod) => { this.inputs.rotation_period = rotationPeriod; }}
               label="Rotation period (ISO8601 Duration)"
               onChange={this._onPeriodUpdate('rotation_period')}
               value={this.state.rotation_period}
               help={'How long an index gets written to before it is rotated. (i.e. "P1D" for 1 day, "PT6H" for 6 hours)'}
               addonAfter={this._formatDuration()}
               bsStyle={this._validationState()}
               required />
      </div>
    );
  }
}

export default TimeBasedRotationStrategyConfiguration;
