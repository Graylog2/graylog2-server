import React from 'react';
import Reflux from 'reflux';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { ConfigurationForm } from 'components/configurationforms';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertConditionsStore } = CombinedProvider.get('AlertConditions');

import AlertConditionsFactory from 'logic/alertconditions/AlertConditionsFactory';

const AlertConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object,
    onSubmit: React.PropTypes.func.isRequired,
    type: React.PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(AlertConditionsStore)],
  getDefaultProps() {
    return {
      alertCondition: {},
      onSubmit: () => {},
    };
  },
  getValue() {
    const values = this.refs.customConfigurationForm ? this.refs.customConfigurationForm.getValue() : this.refs.configurationForm.getValue();
    return {
      title: values.title,
      parameters: values.configuration,
    };
  },
  open() {
    this.refs.configurationForm.open();
  },
  _closeModal() {
    this.ref.modal.close();
  },
  _onSubmit() {
    const request = this.getValue();
    request.type = this.props.type;
    this.props.onSubmit(request);
  },
  alertConditionsFactory: new AlertConditionsFactory(),
  render() {
    const type = this.props.type;
    const alertCondition = this.props.alertCondition;
    const typeDefinition = this.state.types[type];
    const alertConditionTypes = this.alertConditionsFactory.get(type);
    const alertConditionType = alertConditionTypes && alertConditionTypes.length > 0 && alertConditionTypes[0];
    if (!alertConditionType || !alertConditionType.configuration_form) {
      return (<ConfigurationForm ref="configurationForm"
                                 key="configuration-form-alert-condition"
                                 configFields={typeDefinition.requested_configuration}
                                 title={`Create new ${typeDefinition.name}`}
                                 typeName={type}
                                 submitAction={this._onSubmit}
                                 cancelAction={this._handleCancel}
                                 titleValue={alertCondition.title}
                                 values={alertCondition.parameters}/>);
    }

    return (<BootstrapModalForm ref="configurationForm"
                                onCancel={this._closeModal}
                                onSubmitForm={this._onSubmit}
                                submitButtonText="Save">
      <fieldset>
        <input type="hidden" name="type" value={type} />
        <alertConditionType.configuration_form ref="customConfigurationForm" alertCondition={alertCondition} />
      </fieldset>
    </BootstrapModalForm>);
  },
});

export default AlertConditionForm;
