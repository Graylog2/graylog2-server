import React from 'react';
import LinkedStateMixin from 'react-addons-linked-state-mixin';
import jQuery from 'jquery';
import { Well } from 'react-bootstrap';

import { TypeAheadFieldInput } from 'components/common';
import GracePeriodInput from 'components/alertconditions/GracePeriodInput';

const FieldContentConditionForm = React.createClass({
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
  _onChange(event) {
    const state = {};
    state[event.target.name] = event.target.value;
    this.setState(state);
  },
  render() {
    return (
      <Well className="alert-type-form alert-type-form-message-count form-inline well-sm">
        Trigger alert when a message arrives that has the field{' '}
        <TypeAheadFieldInput ref="fieldInput"
                             name="field"
                             type="text"
                             autoComplete="off"
                             valueLink={this.linkState('field')}
                             required />
        <br />
        set to{' '}
        <input ref="value" name="value" type="text" className="form-control" autoComplete="off" required
               value={this.state.value} onChange={this._onChange}/>
        {' '}
        <GracePeriodInput ref="gracePeriod" parameters={{ grace: this.state.grace, backlog: this.state.backlog }}/>
      </Well>
    );
  },
});

export default FieldContentConditionForm;
