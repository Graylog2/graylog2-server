import React from 'react';
import jQuery from 'jquery';
import { Well } from 'react-bootstrap';

import { Pluralize } from 'components/common';
import GracePeriodInput from 'components/alertconditions/GracePeriodInput';

const MessageCountConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object,
    typeDefinition: React.PropTypes.object.isRequired,
  },
  getInitialState() {
    if (this.props.alertCondition) {
      return this.props.alertCondition.parameters;
    }
    const defaultValues = {};
    jQuery.map(this.props.typeDefinition.requested_configuration,
      (definition, field) => {
        defaultValues[field] = definition.default_value;
      });
    return defaultValues;
  },
  getValue() {
    return {
      configuration: jQuery.extend(this.state, this.refs.gracePeriod.getValue()),
    };
  },
  _onChange(event) {
    const state = {};
    state[event.target.name] = event.target.value;
    this.setState(state);
  },
  render() {
    return (
      <Well className="alert-type-form alert-type-form-message-count form-inline well-sm">
        Trigger alert when there are
        {' '}
        <span className="threshold-type">
            <label className="radio-inline">
              <input ref="threshold_type" type="radio" name="threshold_type" onChange={this._onChange} value="more"
                     checked={this.state.thresholdType === 'more'}/>
              more
            </label>

            <label className="radio-inline">
              <input ref="threshold_type" type="radio" name="threshold_type" onChange={this._onChange} value="less"
                     checked={this.state.thresholdType === 'less'}/>
              less
            </label>
          </span>
        <br />
        than{' '}
        <input ref="threshold" name="threshold" type="number" min="0" className="form-control"
               value={this.state.threshold} onChange={this._onChange} required/>
        {' '}
        <Pluralize singular="message" plural="messages" value={this.state.threshold}/> in the last
        {' '}
        <input ref="time" name="time" type="number" min="1" className="form-control"
               value={this.state.time} onChange={this._onChange} required/>
        {' '}
        <Pluralize singular="minute" plural="minutes" value={this.state.time}/>
        {' '}
        <GracePeriodInput ref="gracePeriod" parameters={{ grace: this.state.grace, backlog: this.state.backlog }}/>
      </Well>
    );
  },
});

export default MessageCountConditionForm;
