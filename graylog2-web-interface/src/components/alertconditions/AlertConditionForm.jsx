import React from 'react';
import { Well } from 'react-bootstrap';

import AlertConditionsFactory from 'logic/alertconditions/AlertConditionsFactory';

const AlertConditionForm = React.createClass({
  propTypes: {
    type: React.PropTypes.string.isRequired,
    alertCondition: React.PropTypes.object,
  },
  alertConditionsFactory: new AlertConditionsFactory(),
  getValue() {
    return this.refs.conditionForm.getValue();
  },
  _formatConditionFormFields(type) {
    const typeDefinition = this.alertConditionsFactory.get(type);

    if (typeDefinition !== undefined) {
      return <typeDefinition.configuration_form ref="conditionForm" alertCondition={this.props.alertCondition}/>;
    }

    return undefined;
  },
  render() {
    return (
      <Well className="alert-type-form alert-type-form-message-count form-inline well-sm">
        {this._formatConditionFormFields(this.props.type)}
      </Well>
    );
  }
});

export default AlertConditionForm;
