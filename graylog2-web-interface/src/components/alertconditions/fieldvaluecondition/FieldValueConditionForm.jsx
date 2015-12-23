import React from 'react';
import jQuery from 'jquery';

import GracePeriodInput from 'components/alertconditions/GracePeriodInput';

const FieldValueConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object,
  },
  getDefaultProps() {
    return {
      alertCondition: {
        field: '',
        time: 0,
        threshold: 0,
        threshold_type: 'LOWER',
        type: 'MEAN',
      },
    };
  },
  getInitialState() {
    return {
      thresholdType: this.props.alertCondition.threshold_type,
    };
  },
  getValue() {
    return jQuery.extend({
      field: this.refs.field.value,
      time: Number(this.refs.time.value),
      threshold: parseFloat(this.refs.threshold.value),
      threshold_type: this.state.thresholdType,
      type: this.refs.check_type.value,
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
            <input key={'threshold-type-' + type} ref="threshold_type" type="radio" name="threshold_type" onChange={this._onTypeChanged}
                   value={type} checked={this.state.thresholdType === type}/>
            {type.toLowerCase()}
          </label>
        )}
      </span>
    );
  },
  _onTypeChanged(evt) {
    this.setState({threshold_type: evt.target.value});
  },
  render() {
    const alertCondition = this.props.alertCondition;
    return (
      <span>
        Trigger alert when the field{' '}
        <input ref="field" name="field" type="text" className="form-control typeahead-fields" autoComplete="off" required defaultValue={alertCondition.field}/>
        <br />
        has a {this._formatCheckType()}
        <br />
        that was {this._formatThresholdType()} {' '}
        <input ref="threshold" name="threshold" type="number" className="form-control pluralsingular validatable"
               data-validate="number" data-pluralsingular="threshold-descr" defaultValue={alertCondition.threshold} />
        {' '}<span className="threshold-descr" data-plural="messages" data-singular="message">messages</span>{' '}
        in the last{' '}
        <input ref="time" name="time" type="number" className="form-control pluralsingular validatable"
               data-validate="positive_number" data-pluralsingular="time-descr" defaultValue={alertCondition.time} />{' '}
        <span className="time-descr" data-plural="minutes" data-singular="minute">minutes</span>
        {' '}
        <GracePeriodInput ref="gracePeriod" alertCondition={alertCondition}/>
      </span>
    );
  },
});

export default FieldValueConditionForm;
