import React from 'react';
import Reflux from 'reflux';
import { FormControls } from 'react-bootstrap';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import { ConfigurationForm, TitleField } from 'components/configurationforms';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertConditionsStore } = CombinedProvider.get('AlertConditions');
import { PluginStore } from 'graylog-web-plugin/plugin';

const AlertConditionForm = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object,
    onCancel: React.PropTypes.func,
    onSubmit: React.PropTypes.func.isRequired,
    type: React.PropTypes.string.isRequired,
  },
  mixins: [Reflux.connect(AlertConditionsStore)],
  getDefaultProps() {
    return {
      onCancel: () => {},
      onSubmit: () => {},
    };
  },
  getInitialState() {
    return {
      title: this.props.alertCondition && this.props.alertCondition.title,
    };
  },

  getValue() {
    const values = this.refs.customConfigurationForm ? this.refs.customConfigurationForm.getValue() : this.refs.configurationForm.getValue();
    return {
      title: values.title || this.state.title,
      type: this.props.type,
      parameters: values.configuration,
    };
  },
  open() {
    this.refs.configurationForm.open();
  },
  _handleTitleChange(field, value) {
    this.setState({ title: value });
  },
  _onCancel() {
    this.props.onCancel();
  },
  _onSubmit() {
    const request = this.getValue();
    this.props.onSubmit(request);
    if (typeof this.refs.configurationForm.close === 'function') {
      this.refs.configurationForm.close();
    }
  },
  _formatTitle(alertCondition, name) {
    const action = alertCondition ? 'Update' : 'Create new';
    const conditionName = alertCondition ? <em>{alertCondition.title || 'Untitled'}</em> : name;
    return <span>{action} {conditionName}</span>;
  },

  render() {
    const type = this.props.type;
    const alertCondition = this.props.alertCondition;
    const typeDefinition = this.state.types[type];
    const conditionType = PluginStore.exports('alertConditions').find(c => c.type === type) || {};
    if (!conditionType.formComponent) {
      return (<ConfigurationForm ref="configurationForm"
                                 key="configuration-form-alert-condition"
                                 configFields={typeDefinition.requested_configuration}
                                 title={this._formatTitle(alertCondition, typeDefinition.name)}
                                 typeName={type}
                                 submitAction={this._onSubmit}
                                 cancelAction={this._onCancel}
                                 titleValue={alertCondition ? alertCondition.title : ''}
                                 values={alertCondition ? alertCondition.parameters : {}}>
        <FormControls.Static label="Condition description">{typeDefinition.human_name}</FormControls.Static>
      </ConfigurationForm>);
    }

    return (
      <BootstrapModalForm ref="configurationForm"
                          title={this._formatTitle(alertCondition, typeDefinition.name)}
                          onCancel={this._onCancel}
                          onSubmitForm={this._onSubmit}
                          submitButtonText="Save">
        <fieldset>
          <TitleField typeName={type} value={this.state.title} onChange={this._handleTitleChange} />
          <conditionType.formComponent ref="customConfigurationForm" alertCondition={alertCondition} typeDefinition={typeDefinition} />
        </fieldset>
      </BootstrapModalForm>
    );
  },
});

export default AlertConditionForm;
