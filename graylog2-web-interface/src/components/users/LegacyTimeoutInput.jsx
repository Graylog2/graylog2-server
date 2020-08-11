import PropTypes from 'prop-types';
import React from 'react';
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';

import { Col } from 'components/graylog';
import { Input } from 'components/bootstrap';
import TimeoutUnitSelect from 'components/users/TimeoutUnitSelect';

import { MS_DAY, MS_HOUR, MS_MINUTE, MS_SECOND } from './timeoutConstants';

const TimeoutInput = createReactClass({
  displayName: 'TimeoutInput',

  propTypes: {
    controlSize: PropTypes.number,
    labelSize: PropTypes.number,
    value: PropTypes.number,
    onChange: PropTypes.func,
  },

  getDefaultProps() {
    return {
      value: MS_HOUR,
      labelSize: 2,
      controlSize: 10,
      onChange: () => {},
    };
  },

  getInitialState() {
    const { value } = this.props;
    const unit = this._estimateUnit(value);

    return {
      sessionTimeoutNever: (value ? value === -1 : false),
      value: (value ? Math.floor(value / Number(unit)) : 0),
      unit: unit,
    };
  },

  getValue() {
    const { sessionTimeoutNever, unit } = this.state;

    if (sessionTimeoutNever) {
      return -1;
    }

    return (this.timeout.input.value * Number(unit));
  },

  _estimateUnit(value) {
    if (value === 0) {
      return MS_SECOND;
    }

    if (value % MS_DAY === 0) {
      return MS_DAY;
    }

    if (value % MS_HOUR === 0) {
      return MS_HOUR;
    }

    if (value % MS_MINUTE === 0) {
      return MS_MINUTE;
    }

    return MS_SECOND;
  },

  _onClick(evt) {
    this.setState({ sessionTimeoutNever: evt.target.checked }, this._notifyChange);
  },

  _onChangeValue(evt) {
    this.setState({ value: evt.target.value }, this._notifyChange);
  },

  _onChangeUnit(unit) {
    this.setState({ unit }, this._notifyChange);
  },

  _notifyChange() {
    const { onChange } = this.props;

    if (typeof onChange === 'function') {
      onChange(this.getValue());
    }
  },

  render() {
    const { controlSize, labelSize } = this.props;
    const { sessionTimeoutNever, unit, value } = this.state;

    return (
      <span>
        <Input type="checkbox"
               id="session-timeout-never"
               name="session_timeout_never"
               labelClassName={`col-sm-${controlSize}`}
               wrapperClassName={`col-sm-offset-${labelSize} col-sm-${controlSize}`}
               label="Sessions do not time out"
               help="When checked sessions never time out due to inactivity."
               onChange={this._onClick}
               checked={sessionTimeoutNever} />

        <Input id="timeout-controls"
               label="Timeout"
               help="Session automatically end after this amount of time, unless they are actively used."
               labelClassName={`col-sm-${labelSize}`}
               wrapperClassName={`col-sm-${controlSize}`}>
          <div className="clearfix">
            <Col sm={2}>
              <Input ref={(timeout) => { this.timeout = timeout; }}
                     type="number"
                     id="timeout"
                     className="validatable"
                     name="timeout"
                     min={1}
                     data-validate="positive_number"
                     disabled={sessionTimeoutNever}
                     value={value}
                     onChange={this._onChangeValue} />
            </Col>
            <Col sm={3}>
              <TimeoutUnitSelect ref={(sessionTimeoutUnit) => { this.sessionTimeoutUnit = sessionTimeoutUnit; }}
                                 disabled={sessionTimeoutNever}
                                 value={`${unit}`}
                                 onChange={this._onChangeUnit} />
            </Col>
          </div>
        </Input>
      </span>
    );
  },
});

export default TimeoutInput;
