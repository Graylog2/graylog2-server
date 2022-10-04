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
// eslint-disable-next-line no-restricted-imports
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import lodash from 'lodash';

import { ColorPickerPopover, FormSubmit, Select, SourceCodeEditor } from 'components/common';
import { Button, Col, ControlLabel, FormControl, FormGroup, HelpBlock, Row, Input } from 'components/bootstrap';
import history from 'util/History';
import Routes from 'routing/Routes';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import { CollectorConfigurationsActions } from 'stores/sidecars/CollectorConfigurationsStore';
import { CollectorsActions, CollectorsStore } from 'stores/sidecars/CollectorsStore';

import SourceViewModal from './SourceViewModal';
import ImportsViewModal from './ImportsViewModal';
import ConfigurationTagsSelect from './ConfigurationTagsSelect';

const ConfigurationForm = createReactClass({
  // eslint-disable-next-line react/no-unused-class-component-methods
  displayName: 'ConfigurationForm',
  // eslint-disable-next-line react/no-unused-class-component-methods
  propTypes: {
    action: PropTypes.oneOf(['create', 'edit']),
    configuration: PropTypes.object,
    configurationSidecars: PropTypes.object,
  },

  mixins: [Reflux.connect(CollectorsStore)],

  getDefaultProps() {
    return {
      action: 'edit',
      configuration: {
        color: '#FFFFFF',
        template: '',
      },
      configurationSidecars: {},
    };
  },

  getInitialState() {
    const { configuration } = this.props;

    return {
      error: false,
      validation_errors: {},
      formData: {
        id: configuration.id,
        name: configuration.name,
        color: configuration.color,
        collector_id: configuration.collector_id,
        template: configuration.template || '',
        tags: configuration.tags || [],
      },
    };
  },

  UNSAFE_componentWillMount() {
    this._debouncedValidateFormData = lodash.debounce(this._validateFormData, 200);
  },

  componentDidMount() {
    CollectorsActions.all();
  },

  defaultTemplates: {},

  _isTemplateSet(template) {
    return template !== undefined && template !== '';
  },

  _hasErrors() {
    const { error, formData } = this.state;

    return error || !this._isTemplateSet(formData.template);
  },

  _validateFormData(nextFormData, checkForRequiredFields) {
    CollectorConfigurationsActions.validate(nextFormData).then((validation) => {
      const nextValidation = lodash.clone(validation);

      if (checkForRequiredFields && !this._isTemplateSet(nextFormData.template)) {
        nextValidation.errors.template = ['Please fill out the configuration field.'];
        nextValidation.failed = true;
      }

      this.setState({ validation_errors: nextValidation.errors, error: nextValidation.failed });
    });
  },

  _save() {
    const { action } = this.props;
    const { formData } = this.state;

    if (this._hasErrors()) {
      // Ensure we display an error on the template field, as this is not validated by the browser
      this._validateFormData(formData, true);

      return;
    }

    if (action === 'create') {
      CollectorConfigurationsActions.createConfiguration(formData)
        .then(() => history.push(Routes.SYSTEM.SIDECARS.CONFIGURATION));
    } else {
      CollectorConfigurationsActions.updateConfiguration(formData);
    }
  },

  _formDataUpdate(key) {
    const { formData } = this.state;

    return (nextValue, _, hideCallback) => {
      const nextFormData = lodash.cloneDeep(formData);

      nextFormData[key] = nextValue;
      this._debouncedValidateFormData(nextFormData, false);
      this.setState({ formData: nextFormData }, hideCallback);
    };
  },

  // eslint-disable-next-line react/no-unused-class-component-methods
  replaceConfigurationVariableName(oldname, newname) {
    const { formData } = this.state;

    if (oldname === '' || oldname === newname) {
      return;
    }

    // replaceAll without having to use a Regex
    const updatedTemplate = formData.template.split(`\${user.${oldname}}`).join(`\${user.${newname}}`);

    this._onTemplateChange(updatedTemplate);
  },

  _onNameChange(event) {
    const nextName = event.target.value;

    this._formDataUpdate('name')(nextName);
  },

  _onTagsChange(nextTags) {
    const nextTagsArray = nextTags.split(',');

    this._formDataUpdate('tags')(nextTagsArray);
  },

  _getCollectorDefaultTemplate(collectorId) {
    const storedTemplate = this.defaultTemplates[collectorId];

    if (storedTemplate !== undefined) {
      return new Promise((resolve) => {
        resolve(storedTemplate);
      });
    }

    return CollectorsActions.getCollector(collectorId).then((collector) => {
      this.defaultTemplates[collectorId] = collector.default_template;

      return collector.default_template;
    });
  },

  _onCollectorChange(nextId) {
    const { formData } = this.state;

    // Start loading the request to get the default template, so it is available asap.
    const defaultTemplatePromise = this._getCollectorDefaultTemplate(nextId);

    const nextFormData = lodash.cloneDeep(formData);

    nextFormData.collector_id = nextId;

    // eslint-disable-next-line no-alert
    if (!nextFormData.template || window.confirm('Do you want to use the default template for the selected Configuration?')) {
      // Wait for the promise to resolve and then update the whole formData state
      defaultTemplatePromise.then((defaultTemplate) => {
        this._onTemplateChange(defaultTemplate);
        nextFormData.template = defaultTemplate;
      });
    }

    this.setState({ formData: nextFormData });
  },

  _onTemplateImport(nextTemplate) {
    const { formData } = this.state;

    const nextFormData = lodash.cloneDeep(formData);

    // eslint-disable-next-line no-alert
    if (!nextFormData.template || window.confirm('Do you want to overwrite your current work with this Configuration?')) {
      this._onTemplateChange(nextTemplate);
    }
  },

  _onTemplateChange(nextTemplate) {
    this._formDataUpdate('template')(nextTemplate);
  },

  _onSubmit(event) {
    event.preventDefault();
    this._save();
  },

  _onCancel() {
    history.goBack();
  },

  _onShowSource() {
    this.previewModal.open();
  },

  _onShowImports() {
    this.uploadsModal.open();
  },

  _formatCollector(collector) {
    return collector ? `${collector.name} on ${lodash.upperFirst(collector.node_operating_system)}` : 'Unknown collector';
  },

  _formatCollectorOptions() {
    const { collectors } = this.state;

    const options = [];

    if (collectors) {
      collectors.forEach((collector) => {
        options.push({ value: collector.id, label: this._formatCollector(collector) });
      });
    } else {
      options.push({ value: 'none', label: 'Loading collector list...', disable: true });
    }

    return options;
  },

  // eslint-disable-next-line react/no-unstable-nested-components
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

  // eslint-disable-next-line react/no-unstable-nested-components
  _renderCollectorTypeField(collectorId, collectors, configurationSidecars) {
    const isConfigurationInUse = configurationSidecars.sidecar_ids && configurationSidecars.sidecar_ids.length > 0;

    if (isConfigurationInUse) {
      const collector = collectors ? collectors.find((c) => c.id === collectorId) : undefined;

      return (
        <span>
          <FormControl.Static>{this._formatCollector(collector)}</FormControl.Static>
          <HelpBlock bsClass="warning">
            <b>Note:</b> Log Collector cannot change while the Configuration is in use. Clone the Configuration
            to test it using another Collector.
          </HelpBlock>
        </span>
      );
    }

    return (
      <span>
        <Select inputId="collector_id"
                options={this._formatCollectorOptions()}
                value={collectorId}
                onChange={this._onCollectorChange}
                placeholder="Collector"
                required />
        <HelpBlock>Choose the log collector this configuration is meant for.</HelpBlock>
      </span>
    );
  },

  render() {
    const { collectors, formData } = this.state;
    const { action, configurationSidecars } = this.props;

    return (
      <div>
        <form onSubmit={this._onSubmit}>
          <fieldset>
            <Input type="text"
                   id="name"
                   label="Name"
                   onChange={this._onNameChange}
                   bsStyle={this._validationState('name')}
                   help={this._formatValidationMessage('name', 'Required. Name for this configuration')}
                   value={formData.name || ''}
                   autoFocus
                   required />

            <FormGroup controlId="color">
              <ControlLabel>Configuration color</ControlLabel>
              <div>
                <ColorLabel color={formData.color} />
                <div style={{ display: 'inline-block', marginLeft: 15 }}>
                  <ColorPickerPopover id="color"
                                      placement="right"
                                      color={formData.color}
                                      triggerNode={<Button bsSize="xsmall">Change color</Button>}
                                      onChange={this._formDataUpdate('color')} />
                </div>
              </div>
              <HelpBlock>Choose a color to use for this configuration.</HelpBlock>
            </FormGroup>

            <FormGroup controlId="tags">
              <ControlLabel>Configuration Tags</ControlLabel>
              <div>
                <ConfigurationTagsSelect id="tags"
                                         availableTags={formData.tags.map((tag) => ({ name: tag }))}
                                         tags={formData.tags}
                                         onChange={this._onTagsChange}
                                         className="form-control" />
              </div>
              <HelpBlock>Choose tags to use for this configuration.</HelpBlock>
            </FormGroup>

            <FormGroup controlId="collector_id">
              <ControlLabel>Collector</ControlLabel>
              {this._renderCollectorTypeField(formData.collector_id, collectors, configurationSidecars)}
            </FormGroup>

            <FormGroup controlId="template"
                       validationState={this._validationState('template')}>
              <ControlLabel>Configuration</ControlLabel>
              <SourceCodeEditor id="template"
                                height={400}
                                value={formData.template || ''}
                                onChange={this._onTemplateChange} />
              <Button className="pull-right"
                      bsStyle="link"
                      bsSize="sm"
                      onClick={this._onShowSource}>
                Preview
              </Button>
              <Button className="pull-right"
                      bsStyle="link"
                      bsSize="sm"
                      onClick={this._onShowImports}>
                Migrate
              </Button>
              <HelpBlock>
                {this._formatValidationMessage('template', 'Required. Collector configuration, see quick reference for more information.')}
              </HelpBlock>
            </FormGroup>
          </fieldset>

          <Row>
            <Col md={12}>
              <FormSubmit submitButtonText={`${action === 'create' ? 'Create' : 'Update'} configuration`}
                          disabledSubmit={this._hasErrors()}
                          onCancel={this._onCancel} />
            </Col>
          </Row>
        </form>
        <SourceViewModal ref={(c) => { this.previewModal = c; }}
                         templateString={formData.template} />
        <ImportsViewModal ref={(c) => { this.uploadsModal = c; }}
                          onApply={this._onTemplateImport} />
      </div>
    );
  },
});

export default ConfigurationForm;
