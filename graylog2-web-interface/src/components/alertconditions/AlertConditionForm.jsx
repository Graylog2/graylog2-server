import React from 'react';
import Reflux from 'reflux';

import { ConfigurationForm } from 'components/configurationforms';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertConditionsStore } = CombinedProvider.get('AlertConditions');

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
    const values = this.refs.configurationForm.getValue();
    return {
      title: values.title,
      parameters: values.configuration,
    };
  },
  open() {
    this.refs.configurationForm.open();
  },
  _onSubmit() {
    const request = this.getValue();
    request.type = this.props.type;
    this.props.onSubmit(request);
  },
  render() {
    const type = this.props.type;
    const alertCondition = this.props.alertCondition;
    const typeDefinition = this.state.types[type];
    return (<ConfigurationForm ref="configurationForm"
                               key="configuration-form-alert-condition"
                               configFields={typeDefinition.requested_configuration}
                               title={`Create new ${typeDefinition.human_name}`}
                               typeName={type}
                               submitAction={this._onSubmit}
                               cancelAction={this._handleCancel}
                               titleValue={alertCondition.title}
                               values={alertCondition.parameters}/>);

  },
});

export default AlertConditionForm;
