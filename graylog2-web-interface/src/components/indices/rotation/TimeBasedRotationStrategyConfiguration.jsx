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

const _validationLimit = (durationInMilliseconds, rotationLimit) => {
  return durationInMilliseconds <= moment.duration(rotationLimit).asMilliseconds();
};

class TimeBasedRotationStrategyConfiguration extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
  };

  inputs = {};

  constructor(props) {
    super(props);
    const {
      config: {
        rotation_period: rotationPeriod,
        max_rotation_period: rotationLimit,
        rotate_empty_index_set: rotateEmptyIndexSet,
      },
    } = this.props;

    this.state = {
      rotation_period: rotationPeriod,
      rotationLimit,
      rotate_empty_index_set: rotateEmptyIndexSet,
    };
  }

  _onPeriodUpdate = (field) => {
    const { updateConfig } = this.props;

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
        updateConfig({
          rotation_period: period,
          rotate_empty_index_set: this.state.rotate_empty_index_set,
        });
      }
    };
  };

  _onRotateEmptyIndexSetUpdate = (field) => {
    const { updateConfig } = this.props;

    return () => {
      const update = {};
      const rotateEmptyIndexSet = this.inputs[field].getValue();

      update[field] = rotateEmptyIndexSet;
      this.setState(update);

      updateConfig({
        rotation_period: this.state.rotation_period,
        rotate_empty_index_set: rotateEmptyIndexSet,
      });
    };
  };

  _isValidPeriod = (duration) => {
    const { rotation_period: rotationPeriod, rotationLimit } = this.state;
    const check = duration || rotationPeriod;
    const checkInMilliseconds = moment.duration(check).asMilliseconds();

    return checkInMilliseconds >= 3600000 && (
      rotationLimit ? _validationLimit(checkInMilliseconds, rotationLimit) : true
    );
  };

  _validationState = () => {
    if (this._isValidPeriod()) {
      return undefined;
    }

    return 'error';
  };

  _formatDuration = () => {
    const { rotation_period: rotationPeriod, rotationLimit } = this.state;
    const maxRotationPeriodErrorMessage = rotationLimit ? ` and max ${moment.duration(rotationLimit).humanize()}` : '';

    return this._isValidPeriod() ? moment.duration(rotationPeriod).humanize() : `invalid (min 1 hour${maxRotationPeriodErrorMessage})`;
  };

  render() {
    const {
      rotation_period: rotationPeriod,
      rotate_empty_index_set: rotateEmptyIndexSet,
      rotationLimit,
    } = this.state;
    const maxRotationPeriodHelpText = rotationLimit ? ` The max rotation period is set to ${moment.duration(rotationLimit).humanize()} by Administrator.` : '';

    return (
      <div>
        <Input id="rotation-period"
               type="text"
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9"
               ref={(rotationPeriodRef) => { this.inputs.rotation_period = rotationPeriodRef; }}
               label="Rotation period (ISO8601 Duration)"
               onChange={this._onPeriodUpdate('rotation_period')}
               value={rotationPeriod}
               help={`How long an index gets written to before it is rotated. (i.e. "P1D" for 1 day, "PT6H" for 6 hours).${maxRotationPeriodHelpText}`}
               addonAfter={this._formatDuration()}
               bsStyle={this._validationState()}
               required />
        <Input id="rotate-empty-index-sets"
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9"
               label="Empty index set">
          <Input id="rotate-empty-index-sets-checkbox"
                 type="checkbox"
                 ref={(rotateEmptyIndexSetRef) => { this.inputs.rotate_empty_index_set = rotateEmptyIndexSetRef; }}
                 label="Rotate empty index set"
                 onChange={this._onRotateEmptyIndexSetUpdate('rotate_empty_index_set')}
                 checked={rotateEmptyIndexSet}
                 help="Apply the rotation strategy even when the index set is empty (not recommended)." />
        </Input>
      </div>
    );
  }
}

export default TimeBasedRotationStrategyConfiguration;
