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
import React, { useState, useRef, useEffect } from 'react';
import clone from 'lodash/clone';
import cloneDeep from 'lodash/cloneDeep';
import debounce from 'lodash/debounce';
import upperFirst from 'lodash/upperFirst';

import { ColorPickerPopover, FormSubmit, Select, SourceCodeEditor } from 'components/common';
import { Button, Col, ControlLabel, FormControl, FormGroup, HelpBlock, Row, Input } from 'components/bootstrap';
import Routes from 'routing/Routes';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import { CollectorConfigurationsActions } from 'stores/sidecars/CollectorConfigurationsStore';
import { CollectorsActions } from 'stores/sidecars/CollectorsStore';
import ConfigurationHelper from 'components/sidecars/configuration-forms/ConfigurationHelper';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import SourceViewModal from './SourceViewModal';
import ConfigurationTagsSelect from './ConfigurationTagsSelect';

import type { Collector, Configuration, ConfigurationSidecarsResponse } from '../types';

type Props = {
  action?: string
  configuration?: Configuration
  configurationSidecars?: ConfigurationSidecarsResponse
};

const ConfigurationForm = ({
  action = 'edit',
  configuration = {
    id: '',
    name: '',
    collector_id: '',
    template: '',
    color: '#FFFFFF',
    tags: [],
  },
  configurationSidecars,
}: Props) => {
  const initFormData = {
    id: configuration.id,
    name: configuration.name,
    color: configuration.color,
    collector_id: configuration.collector_id,
    template: configuration.template || '',
    tags: configuration.tags || [],
  };

  const [collectors, setCollectors] = useState<Collector[]>([]);
  const [formData, setFormData] = useState(initFormData);
  const [error, setError] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});
  const [showPreviewModal, setShowPreviewModal] = useState(false);
  const defaultTemplates = useRef({});
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => {
    CollectorsActions.all().then((response) => setCollectors(response.collectors));
  }, []);

  const _isTemplateSet = (template: string) => template !== undefined && template !== '';

  const _hasErrors = () => error || !_isTemplateSet(formData.template);

  const _validateFormData = (nextFormData: Partial<Configuration>, checkForRequiredFields: boolean) => {
    CollectorConfigurationsActions.validate(nextFormData).then((validation) => {
      const nextValidation = clone(validation);

      if (checkForRequiredFields && !_isTemplateSet(nextFormData.template)) {
        // @ts-expect-error
        nextValidation.errors.template = ['Please fill out the configuration field.'];
        nextValidation.failed = true;
      }

      setValidationErrors(nextValidation.errors);
      setError(nextValidation.failed);
    });
  };

  const _save = async () => {
    const isCreate = action === 'create';

    sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS[`CONFIGURATION_${isCreate ? 'CREATED' : 'UPDATED'}`], {
      app_pathname: 'sidecars',
      app_section: 'configuration',
    });

    if (_hasErrors()) {
      // Ensure we display an error on the template field, as this is not validated by the browser
      _validateFormData(formData, true);

      return;
    }

    let promise;

    if (isCreate) {
      promise = CollectorConfigurationsActions.createConfiguration(formData)
        .then(() => history.push(Routes.SYSTEM.SIDECARS.CONFIGURATION));
    } else {
      promise = CollectorConfigurationsActions.updateConfiguration(formData);
    }

    await promise;
  };

  const _debouncedValidateFormData = debounce(_validateFormData, 200);

  const _formDataUpdate = (key: string) => (nextValue, _?: React.ChangeEvent<HTMLInputElement>, hideCallback?: () => void) => {
    const nextFormData = cloneDeep(formData);

    nextFormData[key] = nextValue;
    _debouncedValidateFormData(nextFormData, false);
    setFormData(nextFormData);

    if (hideCallback) {
      hideCallback();
    }
  };

  const _onTemplateChange = (nextTemplate: string) => {
    _formDataUpdate('template')(nextTemplate);
  };

  const replaceConfigurationVariableName = (oldname: string, newname: string) => {
    if (oldname === '' || oldname === newname) {
      return;
    }

    // replaceAll without having to use a Regex
    const updatedTemplate = formData.template.split(`\${user.${oldname}}`).join(`\${user.${newname}}`);

    _onTemplateChange(updatedTemplate);
  };

  const _onNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const nextName = event.target.value;

    _formDataUpdate('name')(nextName);
  };

  const _onTagsChange = (nextTags: string) => {
    const nextTagsArray = nextTags.split(',');

    _formDataUpdate('tags')(nextTagsArray);
  };

  const _getCollectorDefaultTemplate = (collectorId: string) => {
    const storedTemplate = defaultTemplates.current[collectorId];

    if (storedTemplate !== undefined) {
      // eslint-disable-next-line no-promise-executor-return
      return new Promise<string>((resolve) => resolve(storedTemplate));
    }

    return CollectorsActions.getCollector(collectorId).then((collector) => {
      defaultTemplates.current[collectorId] = collector.default_template;

      return collector.default_template;
    });
  };

  const _onCollectorChange = async (nextId: string) => {
    // Start loading the request to get the default template, so it is available asap.
    const defaultTemplate = await _getCollectorDefaultTemplate(nextId);

    const nextFormData = cloneDeep(formData);

    nextFormData.collector_id = nextId;

    // eslint-disable-next-line no-alert
    if (!nextFormData.template || window.confirm('Do you want to use the default template for the selected Configuration?')) {
      _onTemplateChange(defaultTemplate);
      nextFormData.template = defaultTemplate;
    }

    _debouncedValidateFormData(nextFormData, true);
    setFormData(nextFormData);
  };

  const _onSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    _save();
  };

  const _onCancel = () => {
    history.goBack();
  };

  const _onShowSource = () => {
    setShowPreviewModal(true);
  };

  const _formatCollector = (collector: Collector) => (collector ? `${collector.name} on ${upperFirst(collector.node_operating_system)}` : 'Unknown collector');

  const _formatCollectorOptions = () => {
    const options = [];

    if (collectors) {
      collectors.forEach((collector) => {
        options.push({ value: collector.id, label: _formatCollector(collector) });
      });
    } else {
      options.push({ value: 'none', label: 'Loading collector list...', disable: true });
    }

    return options;
  };

  const _formatValidationMessage = (fieldName: string, defaultText: React.ReactNode) => {
    if (validationErrors[fieldName]) {
      return <span>{validationErrors[fieldName][0]}</span>;
    }

    return <span>{defaultText}</span>;
  };

  const _validationState = (fieldName: string) => {
    if (validationErrors[fieldName]) {
      return 'error' as const;
    }

    return null;
  };

  const _renderCollectorTypeField = (collectorId: string, _collectors: Array<Collector>, _configurationSidecars: ConfigurationSidecarsResponse) => {
    const isConfigurationInUse = _configurationSidecars?.sidecar_ids?.length > 0;

    if (isConfigurationInUse) {
      const collector = _collectors ? _collectors.find((c) => c.id === collectorId) : undefined;

      return (
        <>
          <FormControl.Static>{_formatCollector(collector)}</FormControl.Static>
          <HelpBlock bsClass="warning">
            <b>Note:</b> Log Collector cannot change while the Configuration is in use. Clone the Configuration
            to test it using another Collector.
          </HelpBlock>
        </>
      );
    }

    return (
      <>
        <Select inputId="collector_id"
                options={_formatCollectorOptions()}
                value={collectorId}
                onChange={_onCollectorChange}
                placeholder="Collector"
                required />
        <HelpBlock>Choose the log collector this configuration is meant for.</HelpBlock>
      </>
    );
  };

  return (
    <Row className="content">
      <Col md={6}>
        <div>
          <form onSubmit={_onSubmit}>
            <fieldset>
              <Input type="text"
                     id="name"
                     label="Name"
                     onChange={_onNameChange}
                     bsStyle={_validationState('name')}
                     help={_formatValidationMessage('name', 'Required. Name for this configuration')}
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
                                        onChange={_formDataUpdate('color')} />
                  </div>
                </div>
                <HelpBlock>Choose a color to use for this configuration.</HelpBlock>
              </FormGroup>

              <FormGroup controlId="tags">
                <ControlLabel>Configuration Assignment Tags</ControlLabel>
                <div>
                  <ConfigurationTagsSelect availableTags={formData.tags.map((tag) => ({ name: tag }))}
                                           tags={formData.tags}
                                           onChange={_onTagsChange} />
                </div>
                <HelpBlock>Sidecars which are configured with a matching tag will automatically receive this configuration.</HelpBlock>
              </FormGroup>

              <FormGroup controlId="collector_id">
                <ControlLabel>Collector</ControlLabel>
                {_renderCollectorTypeField(formData.collector_id, collectors, configurationSidecars)}
              </FormGroup>

              <FormGroup controlId="template"
                         validationState={_validationState('template')}>
                <ControlLabel>Configuration</ControlLabel>
                <SourceCodeEditor id="template"
                                  height={400}
                                  value={formData.template || ''}
                                  onChange={_onTemplateChange} />
                <Button className="pull-right"
                        bsStyle="link"
                        bsSize="sm"
                        onClick={_onShowSource}>
                  Preview
                </Button>
                <HelpBlock>
                  {_formatValidationMessage('template', 'Required. Collector configuration, see quick reference for more information.')}
                </HelpBlock>
              </FormGroup>
            </fieldset>

            <Row>
              <Col md={12}>
                <FormSubmit submitButtonText={`${action === 'create' ? 'Create' : 'Update'} configuration`}
                            disabledSubmit={_hasErrors()}
                            onCancel={_onCancel} />
              </Col>
            </Row>
          </form>
          <SourceViewModal showModal={showPreviewModal}
                           onHide={() => setShowPreviewModal(false)}
                           templateString={formData.template} />
        </div>
      </Col>
      <Col md={6}>
        <ConfigurationHelper onVariableRename={replaceConfigurationVariableName} />
      </Col>
    </Row>
  );
};

export default ConfigurationForm;
