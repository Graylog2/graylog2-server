/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import lodash from 'lodash';

import { Button, ButtonToolbar, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';
import { Select, SourceCodeEditor } from 'components/common';
import { Input } from 'components/bootstrap';
import history from 'util/History';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

const { CollectorsStore, CollectorsActions } = CombinedProvider.get('Collectors');
const { CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');

const CollectorForm = createReactClass({
  displayName: 'CollectorForm',

  propTypes: {
    action: PropTypes.oneOf(['create', 'edit']),
    collector: PropTypes.object,
  },

  mixins: [Reflux.connect(CollectorsStore)],

  getDefaultProps() {
    return {
      action: 'edit',
      collector: {
        default_template: '',
      },
    };
  },

  getInitialState() {
    const { collector } = this.props;

    return {
      error: false,
      validation_errors: {},
      formData: {
        id: collector.id,
        name: collector.name,
        service_type: collector.service_type,
        node_operating_system: collector.node_operating_system,
        executable_path: collector.executable_path,
        execute_parameters: collector.execute_parameters,
        validation_parameters: collector.validation_parameters,
        default_template: String(collector.default_template),
      },
    };
  },

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillMount() {
    this._debouncedValidateFormData = lodash.debounce(this._validateFormData, 200);
  },

  componentDidMount() {
    CollectorsActions.all();
    CollectorConfigurationsActions.all();
  },

  hasErrors() {
    const { error } = this.state;

    return error;
  },

  _save() {
    const { action } = this.props;
    const { formData } = this.state;

    if (!this.hasErrors()) {
      if (action === 'create') {
        CollectorsActions.create(formData)
          .then(() => history.push(Routes.SYSTEM.SIDECARS.CONFIGURATION));
      } else {
        CollectorsActions.update(formData);
      }
    }
  },

  _formDataUpdate(key) {
    const { formData } = this.state;

    return (nextValue) => {
      const nextFormData = lodash.cloneDeep(formData);

      nextFormData[key] = nextValue;
      this._debouncedValidateFormData(nextFormData);
      this.setState({ formData: nextFormData });
    };
  },

  _validateFormData(nextFormData) {
    if (nextFormData.name && nextFormData.node_operating_system) {
      CollectorsActions.validate(nextFormData).then((validation) => (
        this.setState({ validation_errors: validation.errors, error: validation.failed })
      ));
    }
  },

  _onNameChange(event) {
    const nextName = event.target.value;

    this._formDataUpdate('name')(nextName);
  },

  _onInputChange(key) {
    return (event) => {
      this._formDataUpdate(key)(event.target.value);
    };
  },

  _onSubmit(event) {
    event.preventDefault();
    this._save();
  },

  _onCancel() {
    history.goBack();
  },

  _formatServiceTypes() {
    const options = [];

    options.push({ value: 'exec', label: 'Foreground execution' });
    options.push({ value: 'svc', label: 'Windows service' });

    return options;
  },

  _formatOperatingSystems() {
    const options = [];

    options.push({ value: 'linux', label: 'Linux' });
    options.push({ value: 'windows', label: 'Windows' });

    return options;
  },

  _formatValidationMessage(fieldName, defaultText) {
    const { validation_errors: validationErrors } = this.state;

    if (validationErrors[fieldName]) {
      return <span>{validationErrors[fieldName][0]}</span>;
    }

    return <span>{defaultText}</span>;
  },

  _validationState(fieldName) {
    const { validation_errors: validationErrors } = this.state;

    if (validationErrors[fieldName]) {
      return 'error';
    }

    return null;
  },

  render() {
    const { action } = this.props;
    const { formData } = this.state;

    let validationParameters = '';
    let executeParameters = '';

    if (formData.validation_parameters) {
      validationParameters = formData.validation_parameters;
    }

    if (formData.execute_parameters) {
      executeParameters = formData.execute_parameters;
    }

    return (
      <div>
        <form onSubmit={this._onSubmit}>
          <fieldset>
            <Input type="text"
                   id="name"
                   label="Name"
                   onChange={this._onNameChange}
                   bsStyle={this._validationState('name')}
                   help={this._formatValidationMessage('name', 'Name for this collector')}
                   value={formData.name || ''}
                   autoFocus
                   required />

            <FormGroup controlId="service_type"
                       validationState={this._validationState('service_type')}>
              <ControlLabel>Process management</ControlLabel>
              <Select inputId="service_type"
                      options={this._formatServiceTypes()}
                      value={formData.service_type}
                      onChange={this._formDataUpdate('service_type')}
                      placeholder="Service Type"
                      required />
              <HelpBlock>{this._formatValidationMessage('service_type', 'Choose the service type this collector is meant for.')}</HelpBlock>
            </FormGroup>

            <FormGroup controlId="node_operating_system"
                       validationState={this._validationState('node_operating_system')}>
              <ControlLabel>Operating System</ControlLabel>
              <Select inputId="node_operating_system"
                      options={this._formatOperatingSystems()}
                      value={formData.node_operating_system}
                      onChange={this._formDataUpdate('node_operating_system')}
                      placeholder="Name"
                      required />
              <HelpBlock>{this._formatValidationMessage('node_operating_system', 'Choose the operating system this collector is meant for.')}</HelpBlock>
            </FormGroup>

            <Input type="text"
                   id="executablePath"
                   label="Executable Path"
                   onChange={this._onInputChange('executable_path')}
                   bsStyle={this._validationState('executable_path')}
                   help={this._formatValidationMessage('executable_path', 'Path to the collector executable')}
                   value={formData.executable_path || ''}
                   required />

            <Input type="text"
                   id="executeParameters"
                   label={<span>Execute Parameters <small className="text-muted">(Optional)</small></span>}
                   onChange={this._onInputChange('execute_parameters')}
                   help={<span>Parameters the collector is started with.<strong> %s will be replaced by the path to the configuration file.</strong></span>}
                   value={executeParameters || ''} />

            <Input type="text"
                   id="validationParameters"
                   label={<span>Parameters for Configuration Validation <small className="text-muted">(Optional)</small></span>}
                   onChange={this._onInputChange('validation_parameters')}
                   help={<span>Parameters that validate the configuration file. <strong> %s will be replaced by the path to the configuration file.</strong></span>}
                   value={validationParameters || ''} />

            <FormGroup controlId="defaultTemplate">
              <ControlLabel><span>Default Template <small className="text-muted">(Optional)</small></span></ControlLabel>
              <SourceCodeEditor id="template"
                                value={formData.default_template}
                                onChange={this._formDataUpdate('default_template')} />
              <HelpBlock>The default Collector configuration.</HelpBlock>
            </FormGroup>
          </fieldset>

          <Row>
            <Col md={12}>
              <FormGroup>
                <ButtonToolbar>
                  <Button type="submit" bsStyle="primary" disabled={this.hasErrors()}>
                    {action === 'create' ? 'Create' : 'Update'}
                  </Button>
                  <Button type="button" onClick={this._onCancel}>
                    {action === 'create' ? 'Cancel' : 'Back'}
                  </Button>
                </ButtonToolbar>
              </FormGroup>
            </Col>
          </Row>
        </form>
      </div>
    );
  },
});

export default CollectorForm;
