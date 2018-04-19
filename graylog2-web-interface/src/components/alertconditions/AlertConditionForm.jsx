import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import { ControlLabel, FormControl, FormGroup } from 'react-bootstrap';

import { ConfigurationForm } from 'components/configurationforms';

import CombinedProvider from 'injection/CombinedProvider';
const { AlertConditionsStore } = CombinedProvider.get('AlertConditions');

const AlertConditionForm = createReactClass({
  displayName: 'AlertConditionForm',

  propTypes: {
    alertCondition: PropTypes.object,
    onCancel: PropTypes.func,
    onSubmit: PropTypes.func.isRequired,
    type: PropTypes.string.isRequired,
  },

  mixins: [Reflux.connect(AlertConditionsStore)],

  getDefaultProps() {
    return {
      onCancel: () => {
      },
      onSubmit: () => {
      },
    };
  },

  getValue() {
    const values = this.configurationForm.getValue();
    return {
      title: values.title,
      type: this.props.type,
      parameters: values.configuration,
    };
  },

  open() {
    this.configurationForm.open();
  },

  _onCancel() {
    this.props.onCancel();
  },

  _onSubmit() {
    const request = this.getValue();
    this.props.onSubmit(request);
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

    return (
      <ConfigurationForm ref={(configurationForm) => { this.configurationForm = configurationForm; }}
                         key="configuration-form-alert-condition"
                         configFields={typeDefinition.requested_configuration}
                         title={this._formatTitle(alertCondition, typeDefinition.name)}
                         typeName={type}
                         submitAction={this._onSubmit}
                         cancelAction={this._onCancel}
                         titleValue={alertCondition ? alertCondition.title : ''}
                         helpBlock="The alert condition title"
                         values={alertCondition ? alertCondition.parameters : {}}>
        <FormGroup>
          <ControlLabel>{`${typeDefinition.name} description`}</ControlLabel>
          <FormControl.Static>{typeDefinition.human_name}</FormControl.Static>
        </FormGroup>
      </ConfigurationForm>
    );
  },
});

export default AlertConditionForm;
