import React from 'react';
import jQuery from 'jquery';
import { Well } from 'react-bootstrap';

import { Pluralize } from 'components/common';
import GracePeriodInput from 'components/alertconditions/GracePeriodInput';

const MessageCountConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object,
  },
  getDefaultProps() {
    return {
      alertCondition: {
        threshold_type: 'more',
        threshold: 0,
        time: 1,
      },
    };
  },
  getInitialState() {
    return {
      thresholdType: this.props.alertCondition.threshold_type,
      threshold: this.props.alertCondition.threshold,
      time: this.props.alertCondition.time,
    };
  },
  getValue() {
    return jQuery.extend({
      configuration: {
        threshold_type: this.state.thresholdType,
        threshold: Number(this.refs.threshold.value),
        time: Number(this.refs.time.value),
      },
    }, this.refs.gracePeriod.getValue());
  },
  _onTypeChanged(event) {
    this.setState({thresholdType: event.target.value});
  },
  _onThresholdChange(event) {
    this.setState({threshold: event.target.value});
  },
  _onTimeChange(event) {
    this.setState({time: event.target.value});
  },
  render() {
    const alertCondition = this.props.alertCondition;
    return (
      <Well className="alert-type-form alert-type-form-message-count form-inline well-sm">
        Trigger alert when there are
        {' '}
        <span className="threshold-type">
            <label className="radio-inline">
              <input ref="threshold_type" type="radio" name="threshold_type" onChange={this._onTypeChanged} value="more"
                     checked={this.state.thresholdType === 'more'}/>
              more
            </label>

            <label className="radio-inline">
              <input ref="threshold_type" type="radio" name="threshold_type" onChange={this._onTypeChanged} value="less"
                     checked={this.state.thresholdType === 'less'}/>
              less
            </label>
          </span>
        <br />
        than{' '}
        <input ref="threshold" name="threshold" type="number" min="0" className="form-control"
               defaultValue={alertCondition.threshold} onChange={this._onThresholdChange} required/>
        {' '}
        <Pluralize singular="message" plural="messages" value={this.state.threshold}/> in the last
        {' '}
        <input ref="time" name="time" type="number" min="1" className="form-control"
               defaultValue={alertCondition.time} onChange={this._onTimeChange} required/>
        {' '}
        <Pluralize singular="minute" plural="minutes" value={this.state.time}/>
        {' '}
        <GracePeriodInput ref="gracePeriod" {...this.props}/>
      </Well>
    );
  },
});

export default MessageCountConditionForm;
