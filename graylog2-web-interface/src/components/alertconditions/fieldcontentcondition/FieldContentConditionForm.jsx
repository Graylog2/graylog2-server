import React from 'react';
import jQuery from 'jquery';
import { Well } from 'react-bootstrap';

import {TypeAheadFieldInput} from 'components/common';
import GracePeriodInput from 'components/alertconditions/GracePeriodInput';

const FieldContentConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object,
  },
  getDefaultProps() {
    return {
      alertCondition: {
        parameters: {
          field: '',
          value: '',
        },
      },
    };
  },
  getInitialState() {
    return {
      field: this.props.alertCondition.parameters.field,
    };
  },
  getValue() {
    return jQuery.extend({
      configuration: {
        field: this.state.field,
        value: this.refs.value.value,
      },
    }, this.refs.gracePeriod.getValue());
  },
  _onFieldChange(event) {
    this.setState({ field: event.target.value });
  },
  render() {
    const alertCondition = this.props.alertCondition;
    return (
      <Well className="alert-type-form alert-type-form-message-count form-inline well-sm">
        Trigger alert when a message arrives that has the field{' '}
        <TypeAheadFieldInput ref="fieldInput"
                             type="text"
                             autoComplete="off"
                             defaultValue={alertCondition.parameters.field}
                             onChange={this._onFieldChange}
                             required />
        <br />
        set to{' '}
        <input ref="value" type="text" className="form-control" autoComplete="off" required defaultValue={alertCondition.parameters.value}/>
        {' '}
        <GracePeriodInput ref="gracePeriod" alertCondition={alertCondition}/>
      </Well>
    );
  },
});

export default FieldContentConditionForm;
