import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Row, Col } from 'react-bootstrap';

import { Input } from 'components/bootstrap';
import TimeoutUnitSelect from 'components/users/TimeoutUnitSelect';

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
      value: 60 * 60 * 1000,
      labelSize: 2,
      controlSize: 10,
    };
  },

  getInitialState() {
    const unit = this._estimateUnit(this.props.value);
    return {
      sessionTimeoutNever: (this.props.value ? this.props.value === -1 : false),
      value: (this.props.value ? Math.floor(this.props.value / unit) : 0),
      unit: unit,
    };
  },

  getValue() {
    if (this.state.sessionTimeoutNever) {
      return -1;
    }
    return (this.timeout.value * this.session_timeout_unit.getValue());
  },

  MS_DAY: 24 * 60 * 60 * 1000,
  MS_HOUR: 60 * 60 * 1000,
  MS_MINUTE: 60 * 1000,
  MS_SECOND: 1000,

  _estimateUnit(value) {
    if (value === 0) {
      return this.MS_SECOND;
    }

    if (value % this.MS_DAY === 0) {
      return this.MS_DAY;
    }

    if (value % this.MS_HOUR === 0) {
      return this.MS_HOUR;
    }

    if (value % this.MS_MINUTE === 0) {
      return this.MS_MINUTE;
    }

    return this.MS_SECOND;
  },

  _onClick(evt) {
    this.setState({ sessionTimeoutNever: evt.target.checked }, this._notifyChange);
  },

  _onChangeValue(evt) {
    this.setState({ value: evt.target.value }, this._notifyChange);
  },

  _onChangeUnit(evt) {
    this.setState({ unit: evt.target.value }, this._notifyChange);
  },

  _notifyChange() {
    if (typeof this.props.onChange === 'function') {
      this.props.onChange(this.getValue());
    }
  },

  render() {
    return (
      <span>
        <Input type="checkbox" id="session-timeout-never" name="session_timeout_never"
               labelClassName={`col-sm-${this.props.controlSize}`} wrapperClassName={`col-sm-offset-${this.props.labelSize} col-sm-${this.props.controlSize}`}
               label="Sessions do not time out" help="When checked sessions never time out due to inactivity."
               onChange={this._onClick} checked={this.state.sessionTimeoutNever} />

        <Input id="timeout-controls"
               label="Timeout"
               help="Session automatically end after this amount of time, unless they are actively used."
               labelClassName={`col-sm-${this.props.labelSize}`}
               wrapperClassName={`col-sm-${this.props.controlSize}`}>
          <Row>
            <Col sm={2}>
              <input ref={(timeout) => { this.timeout = timeout; }} type="number" id="timeout"
                     className="session-timeout-fields validatable form-control"
                     name="timeout" min={1} data-validate="positive_number" disabled={this.state.sessionTimeoutNever}
                     value={this.state.value} onChange={this._onChangeValue} />
            </Col>
            <Col sm={3}>
              <TimeoutUnitSelect ref={(session_timeout_unit) => { this.session_timeout_unit = session_timeout_unit; }} className="form-control session-timeout-fields"
                                 disabled={this.state.sessionTimeoutNever}
                                 value={this.state.unit} onChange={this._onChangeUnit} />
            </Col>
          </Row>
        </Input>
      </span>
    );
  },
});

export default TimeoutInput;
