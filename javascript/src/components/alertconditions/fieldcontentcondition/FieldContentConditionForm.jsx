import React from 'react';
import jQuery from 'jquery';

import GracePeriodInput from 'components/alertconditions/GracePeriodInput';

const FieldContentConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object,
  },
  getDefaultProps() {
    return {
      alertCondition: {
        field: '',
        value: '',
      },
    };
  },
  getValue() {
    return jQuery.extend({
      field: this.refs.field.value,
      value: this.refs.value.value,
    }, this.refs.gracePeriod.getValue());
  },
  render() {
    const alertCondition = this.props.alertCondition;
    return (
      <span>
        Trigger alert when a message arrives that has the field{' '}
        <input ref="field" type="text" className="form-control typeahead-fields" autoComplete="off" required defaultValue={alertCondition.field}/>
        <br />
        set to{' '}
        <input ref="value" type="text" className="form-control typeahead-fields" autoComplete="off" required defaultValue={alertCondition.value}/>
        {' '}
        <GracePeriodInput ref="gracePeriod" alertCondition={alertCondition}/>
      </span>
    );
  },
});

export default FieldContentConditionForm;
