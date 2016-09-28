import React from 'react';
import jQuery from 'jquery';
import { Well } from 'react-bootstrap';

import {Pluralize, TypeAheadFieldInput} from 'components/common';
import GracePeriodInput from 'components/alertconditions/GracePeriodInput';

const FieldValueConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object,
  },
  getDefaultProps() {
    return {
      alertCondition: {
        field: '',
        time: 1,
        threshold: 0,
        threshold_type: 'LOWER',
        type: 'MEAN',
      },
    };
  },
  getInitialState() {
    return {
      thresholdType: this.props.alertCondition.threshold_type,
      field: this.props.alertCondition.field,
      time: this.props.alertCondition.time,
    };
  },
  getValue() {
    return jQuery.extend({
      configuration: {
        field: this.state.field,
        time: Number(this.refs.time.value),
        threshold: parseFloat(this.refs.threshold.value),
        threshold_type: this.state.thresholdType,
        type: this.refs.check_type.value,
      },
    }, this.refs.gracePeriod.getValue());
  },
  checkTypes: {
    MEAN: 'mean value',
    MIN: 'min value',
    MAX: 'max value',
    SUM: 'sum',
    STDDEV: 'standard deviation',
  },
  thresholdTypes: ['LOWER', 'HIGHER'],
  _formatCheckType() {
    return (
      <select ref="check_type" name="type" className="form-control" defaultValue={this.props.alertCondition.type}>
        {jQuery.map(this.checkTypes, (description, value) => <option key={'threshold-type-' + value} value={value}>{description}</option>)}
      </select>
    );
  },
  _formatThresholdType() {
    return (
      <span className="threshold-type">
        {this.thresholdTypes.map((type) =>
          <label key={'threshold-label-' + type} className="radio-inline">
            <input key={'threshold-type-' + type} ref="threshold_type" type="radio" name="threshold_type" onChange={this._onTypeChange}
                   value={type} checked={this.state.thresholdType === type}/>
            {type.toLowerCase()}
          </label>
        )}
      </span>
    );
  },
  _onFieldChange(event) {
    this.setState({field: event.target.value});
  },
  _onTypeChange(event) {
    this.setState({thresholdType: event.target.value});
  },
  _onTimeChange(event) {
    this.setState({time: event.target.value});
  },
  render() {
    const alertCondition = this.props.alertCondition;
    return (
      <Well className="alert-type-form alert-type-form-message-count form-inline well-sm">
        Trigger alert when the field
        {' '}
        <TypeAheadFieldInput ref="fieldInput"
                             type="text"
                             autoComplete="off"
                             defaultValue={alertCondition.field}
                             onChange={this._onFieldChange}
                             required />
        <br />
        has a {this._formatCheckType()}
        <br />
        that was {this._formatThresholdType()} than{' '}
        <input ref="threshold" name="threshold" step="0.01" type="number" className="form-control"
               defaultValue={alertCondition.threshold} required/>
        {' '}in the last{' '}
        <input ref="time" name="time" type="number" min="1" className="form-control"
               defaultValue={alertCondition.time} onChange={this._onTimeChange} required/>
        {' '}
        <Pluralize singular="minute" plural="minutes" value={this.state.time}/>
        {' '}
        <GracePeriodInput ref="gracePeriod" alertCondition={alertCondition}/>
      </Well>
    );
  },
});

export default FieldValueConditionForm;
