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
import React from 'react';
import cloneDeep from 'lodash/cloneDeep';
import debounce from 'lodash/debounce';
import upperCase from 'lodash/upperCase';
import { Trans, withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';
import i18n from 'i18n';

import { FormSubmit, Select, SourceCodeEditor } from 'components/common';
import { Col, ControlLabel, FormGroup, HelpBlock, Row, Input } from 'components/bootstrap';
import Routes from 'routing/Routes';
import { fetchAllConfigurations } from 'hooks/useCollectorConfigurations';
import { fetchCollectorsAll, createCollector, updateCollector, validateCollector } from 'hooks/useCollectors';
import type { HistoryContext } from 'routing/withHistory';
import withHistory from 'routing/withHistory';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import withTelemetry from 'logic/telemetry/withTelemetry';

type ValidationMessageProps = {
  validationErrors: any;
  fieldName: string;
  defaultText: string;
};

const ValidationMessage = ({ validationErrors, fieldName, defaultText }: ValidationMessageProps) => {
  if (validationErrors[fieldName]) {
    return <span>{validationErrors[fieldName][0]}</span>;
  }

  return <span>{defaultText}</span>;
};

const formatServiceTypes = () => {
  const options = [];

  options.push({
    value: 'exec',
    label: i18n.t('configuration_forms.collector_form.service_type_exec', { ns: 'sidecars' }),
  });
  options.push({
    value: 'svc',
    label: i18n.t('configuration_forms.collector_form.service_type_svc', { ns: 'sidecars' }),
  });

  return options;
};

const formatOperatingSystems = () => {
  const options = [];

  options.push({ value: 'linux', label: 'Linux' });
  options.push({ value: 'windows', label: 'Windows' });
  options.push({ value: 'darwin', label: 'Darwin' });
  options.push({ value: 'freebsd', label: 'FreeBSD' });

  return options;
};

type CollectorFormProps = HistoryContext &
  WithTranslation & {
    action?: 'create' | 'edit';
    collector?: any;
    sendTelemetry?: (...args: any[]) => void;
  };

class CollectorForm extends React.Component<
  CollectorFormProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    action: 'edit',
    collector: {
      default_template: '',
    },
    sendTelemetry: () => {},
  };

  constructor(props) {
    super(props);
    const { collector } = this.props;

    this.state = {
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
  }

  UNSAFE_componentWillMount() {
    this._debouncedValidateFormData = debounce(this._validateFormData, 200);
  }

  componentDidMount() {
    fetchCollectorsAll();
    fetchAllConfigurations();
  }

  private _debouncedValidateFormData: (formData: React.FormEvent) => void;

  hasErrors = () => {
    const { error } = this.state;

    return error;
  };

  _save = () => {
    const { action, history, sendTelemetry } = this.props;
    const { formData } = this.state;
    const isCreate = action === 'create';

    if (!this.hasErrors()) {
      let promise;

      if (isCreate) {
        promise = createCollector(formData).then(() => history.push(Routes.SYSTEM.SIDECARS.CONFIGURATION));
      } else {
        promise = updateCollector(formData);
      }

      promise.then(() => {
        sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS[`LOG_COLLECTOR_${isCreate ? 'CREATED' : 'UPDATED'}`], {
          app_pathname: 'sidecars',
        });
      });
    }
  };

  handleSelectTelemetry = (key, value) => {
    const { sendTelemetry } = this.props;

    if (key === 'service_type' || key === 'node_operating_system') {
      sendTelemetry(TELEMETRY_EVENT_TYPE.SIDECARS[`LOG_COLLECTOR_${upperCase(key).replace(/\s|\//g, '_')}_CHANGED`], {
        app_pathname: 'sidecars',
        event_details: {
          [key]: value,
        },
      });
    }
  };

  _formDataUpdate = (key) => {
    const { formData } = this.state;

    return (nextValue) => {
      const nextFormData = cloneDeep(formData);
      this.handleSelectTelemetry(key, nextValue);

      nextFormData[key] = nextValue;
      this._debouncedValidateFormData(nextFormData);
      this.setState({ formData: nextFormData });
    };
  };

  _validateFormData = (nextFormData) => {
    if (nextFormData.name && nextFormData.node_operating_system) {
      validateCollector(nextFormData).then((validation) =>
        this.setState({ validation_errors: validation.errors, error: validation.failed }),
      );
    }
  };

  _onNameChange = (event) => {
    const nextName = event.target.value;

    this._formDataUpdate('name')(nextName);
  };

  _onInputChange = (key) => (event) => {
    this._formDataUpdate(key)(event.target.value);
  };

  _onSubmit = (event) => {
    event.preventDefault();
    this._save();
  };

  _onCancel = () => {
    const { history } = this.props;
    history.goBack();
  };

  _validationState = (fieldName) => {
    const { validation_errors: validationErrors } = this.state;

    if (validationErrors[fieldName]) {
      return 'error' as const;
    }

    return null;
  };

  render() {
    const { action, t } = this.props;
    const { formData, validation_errors: validationErrors } = this.state;

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
            <Input
              type="text"
              id="name"
              label={t('configuration_forms.collector_form.name')}
              onChange={this._onNameChange}
              bsStyle={this._validationState('name')}
              help={
                <ValidationMessage
                  fieldName="name"
                  defaultText={t('configuration_forms.collector_form.name_help')}
                  validationErrors={validationErrors}
                />
              }
              value={formData.name || ''}
              autoFocus
              required
            />

            <FormGroup controlId="service_type" validationState={this._validationState('service_type')}>
              <ControlLabel>{t('configuration_forms.collector_form.process_management')}</ControlLabel>
              <Select
                inputId="service_type"
                options={formatServiceTypes()}
                value={formData.service_type}
                onChange={this._formDataUpdate('service_type')}
                placeholder={t('configuration_forms.collector_form.service_type')}
                required
              />
              <HelpBlock>
                <ValidationMessage
                  fieldName="service_type"
                  defaultText={t('configuration_forms.collector_form.service_type_help')}
                  validationErrors={validationErrors}
                />
              </HelpBlock>
            </FormGroup>

            <FormGroup
              controlId="node_operating_system"
              validationState={this._validationState('node_operating_system')}>
              <ControlLabel>{t('configuration_forms.collector_form.operating_system')}</ControlLabel>
              <Select
                inputId="node_operating_system"
                options={formatOperatingSystems()}
                value={formData.node_operating_system}
                onChange={this._formDataUpdate('node_operating_system')}
                placeholder={t('configuration_forms.collector_form.name')}
                required
              />
              <HelpBlock>
                <ValidationMessage
                  fieldName="node_operating_system"
                  defaultText={t('configuration_forms.collector_form.operating_system_help')}
                  validationErrors={validationErrors}
                />
              </HelpBlock>
            </FormGroup>

            <Input
              type="text"
              id="executablePath"
              label={t('configuration_forms.collector_form.executable_path')}
              onChange={this._onInputChange('executable_path')}
              bsStyle={this._validationState('executable_path')}
              help={
                <ValidationMessage
                  fieldName="executable_path"
                  defaultText={t('configuration_forms.collector_form.executable_path_help')}
                  validationErrors={validationErrors}
                />
              }
              value={formData.executable_path || ''}
              required
            />

            <Input
              type="text"
              id="executeParameters"
              label={
                <span>
                  {t('configuration_forms.collector_form.execute_parameters')}{' '}
                  <small className="text-muted">{t('configuration_forms.collector_form.optional')}</small>
                </span>
              }
              onChange={this._onInputChange('execute_parameters')}
              help={
                <span>
                  <Trans
                    t={t}
                    i18nKey="configuration_forms.collector_form.execute_parameters_help"
                    components={{ strong: <strong /> }}
                  />
                </span>
              }
              value={executeParameters || ''}
            />

            <Input
              type="text"
              id="validationParameters"
              label={
                <span>
                  {t('configuration_forms.collector_form.validation_parameters')}{' '}
                  <small className="text-muted">{t('configuration_forms.collector_form.optional')}</small>
                </span>
              }
              onChange={this._onInputChange('validation_parameters')}
              help={
                <span>
                  <Trans
                    t={t}
                    i18nKey="configuration_forms.collector_form.validation_parameters_help"
                    components={{ strong: <strong /> }}
                  />
                </span>
              }
              value={validationParameters || ''}
            />

            <FormGroup controlId="defaultTemplate">
              <ControlLabel>
                <span>
                  {t('configuration_forms.collector_form.default_template')}{' '}
                  <small className="text-muted">{t('configuration_forms.collector_form.optional')}</small>
                </span>
              </ControlLabel>
              <SourceCodeEditor
                id="template"
                value={formData.default_template || ''}
                onChange={this._formDataUpdate('default_template')}
              />
              <HelpBlock>{t('configuration_forms.collector_form.default_template_help')}</HelpBlock>
            </FormGroup>
          </fieldset>

          <Row>
            <Col md={12}>
              <FormSubmit
                submitButtonText={
                  action === 'create'
                    ? t('configuration_forms.collector_form.create_collector')
                    : t('configuration_forms.collector_form.update_collector')
                }
                disabledSubmit={this.hasErrors()}
                onCancel={this._onCancel}
              />
            </Col>
          </Row>
        </form>
      </div>
    );
  }
}

export default withTranslation('sidecars')(withTelemetry(withHistory(CollectorForm), 'configuration'));
