import React from 'react';
import LinkedStateMixin from 'react-addons-linked-state-mixin';
import jQuery from 'jquery';
import { Well } from 'react-bootstrap';

import { Pluralize, TypeAheadFieldInput } from 'components/common';
import GracePeriodInput from 'components/alertconditions/GracePeriodInput';

const FieldValueConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object,
    typeDefinition: React.PropTypes.object.isRequired,
  },
  mixins: [LinkedStateMixin],
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
  _formatCheckType() {
    const checkTypes = this.props.typeDefinition.requested_configuration.type.additional_info.values;
    return (
      <select ref="check_type" name="type" className="form-control" value={this.state.type} onChange={this._onChange}>
        {jQuery.map(checkTypes, (description, value) => <option key={`threshold-type-${value}`} value={value}>{description}</option>)}
      </select>
    );
  },
  _formatThresholdType() {
    const thresholdTypes = this.props.typeDefinition.requested_configuration.threshold_type.additional_info.values;
    return (
      <span className="threshold-type">
        {jQuery.map(thresholdTypes, (description, value) =>
          <label key={`threshold-label-${value}`} className="radio-inline">
            <input key={`threshold-type-${value}`} ref="threshold_type" type="radio" name="threshold_type" onChange={this._onChange}
                   value={value} checked={this.state.threshold_type === value}/>
            {description}
          </label>
        )}
      </span>
    );
  },
  _onChange(event) {
    const state = {};
    state[event.target.name] = event.target.value;
    this.setState(state);
  },
  render() {
    return (
      <Well className="alert-type-form alert-type-form-message-count form-inline well-sm">
        Trigger alert when the field
        {' '}
        <TypeAheadFieldInput ref="fieldInput"
                             type="text"
                             name="field"
                             autoComplete="off"
                             valueLink={this.linkState('field')}
                             required />
        <br />
        has a {this._formatCheckType()}
        <br />
        that was {this._formatThresholdType()} than{' '}
        <input ref="threshold" name="threshold" step="0.01" type="number" className="form-control"
               value={this.state.threshold} onChange={this._onChange} required/>
        {' '}in the last{' '}
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

export default FieldValueConditionForm;
