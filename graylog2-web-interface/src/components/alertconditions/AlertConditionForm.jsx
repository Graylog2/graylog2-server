import React from 'react';
import { Well } from 'react-bootstrap';

import AlertConditionsFactory from 'logic/alertconditions/AlertConditionsFactory';

const AlertConditionForm = React.createClass({
  propTypes: {
    type: React.PropTypes.string.isRequired,
    alertCondition: React.PropTypes.object,
  },
  getDefaultProps() {
    return {
      alertCondition: {
        parameters: {},
      },
    };
  },
  getValue() {
    return {
      title: this.refs.title.value,
      parameters: this.refs.conditionForm.getValue(),
    };
  },
  alertConditionsFactory: new AlertConditionsFactory(),

  _formatConditionFormFields(type) {
    const typeDefinition = this.alertConditionsFactory.get(type);

    if (typeDefinition !== undefined) {
      return <typeDefinition.configuration_form ref="conditionForm" alertCondition={this.props.alertCondition.parameters}/>;
    }

    return undefined;
  },
  render() {
    const alertCondition = this.props.alertCondition || { parameters: {} };
    return (
      <Well className="alert-type-form alert-type-form-message-count form-inline well-sm">
        Title: <input ref="title" type="text" className="form-control" autoComplete="off" defaultValue={alertCondition.title}/>
        <span style={{ color: '#939393', marginLeft: 10 }}>
          <small>This title can be included in alert notifications.</small>
        </span>

        <p />
        {this._formatConditionFormFields(this.props.type)}
      </Well>
    );
  },
});

export default AlertConditionForm;
