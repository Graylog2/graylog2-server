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
import { PluginStore } from 'graylog-web-plugin/plugin';
import isNumber from 'lodash/isNumber';

import i18n from 'i18n';

import { Select, HoverForHelp } from 'components/common';
import {
  Button,
  ButtonToolbar,
  Col,
  ControlLabel,
  FormControl,
  FormGroup,
  HelpBlock,
  InputGroup,
  Input,
  Row,
} from 'components/bootstrap';
import EventKeyHelpPopover from 'components/event-definitions/common/EventKeyHelpPopover';
import * as FormsUtils from 'util/FormsUtils';
import withTelemetry from 'logic/telemetry/withTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import commonStyles from '../common/commonStyles.css';
import { fieldValueProviderTitle } from '../pluginTitles';

const t = (key: string, options = {}) => i18n.t(key, { ns: 'event_definitions', ...options });

const requiredFields = ['fieldName'];

const getProviderPlugin = (type) => {
  if (type === undefined) {
    return undefined;
  }

  return PluginStore.exports('fieldValueProviders').find((edt) => edt.type === type);
};

const getConfigProviderType = (config, defaultValue?) => config?.providers?.[0]?.type ?? defaultValue;

const formatFieldValueProviders = () =>
  PluginStore.exports('fieldValueProviders').map((type) => ({ label: fieldValueProviderTitle(type), value: type.type }));

type FieldFormProps = {
  fieldName?: string;
  config?: any;
  currentUser: any;
  keys: any[];
  onChange: (...args: any[]) => void;
  onCancel: (...args: any[]) => void;
  sendTelemetry: (...args: any[]) => void;
};

class FieldForm extends React.Component<
  FieldFormProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    fieldName: '',
    config: {},
  };

  constructor(props) {
    super(props);
    const keyIndex = props.keys.indexOf(props.fieldName);

    this.state = {
      fieldName: props.fieldName,
      config: { data_type: 'string', providers: [], ...props.config },
      isKey: keyIndex >= 0,
      keyPosition: keyIndex >= 0 ? keyIndex + 1 : props.keys.length + 1,
      validation: { errors: {} },
    };
  }

  validate = () => {
    const { isKey, keyPosition, config } = this.state;
    const errors: Record<string, string> = {};

    const providerType = getConfigProviderType(config);
    let pluginRequiredFields: Array<string> = [];

    if (providerType) {
      const providerPlugin = getProviderPlugin(providerType);

      pluginRequiredFields = providerPlugin?.requiredFields;
    }

    requiredFields.forEach((requiredField) => {
      if (!this.state[requiredField]) {
        errors[requiredField] = t('validation.field_cannot_be_empty');
      }
    });

    if (!config.providers[0]?.type) {
      errors['config.providers[0].type'] = t('validation.field_cannot_be_empty');
    }

    if (isKey && (!isNumber(keyPosition) || Number(keyPosition) < 1)) {
      errors.key_position = t('validation.field_positive_number');
    }

    pluginRequiredFields.forEach((requiredField) => {
      if (!config?.providers?.[0]?.[requiredField]) {
        errors[requiredField] = t('validation.field_cannot_be_empty');
      }
    });

    const errorNumber = Object.keys(errors).length;

    if (errorNumber > 0) {
      this.setState({ validation: { errors: errors } });
    }

    return errorNumber === 0;
  };

  handleSubmit = () => {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.DONE_CLICKED, {
      app_action_value: 'done-button',
    });

    if (this.validate()) {
      const { fieldName: prevFieldName, onChange } = this.props;
      const { fieldName, config, isKey, keyPosition } = this.state;

      onChange(prevFieldName, fieldName, config, isKey, keyPosition - 1);
    }
  };

  handleCancel = () => {
    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.CANCEL_CLICKED, {
      app_action_value: 'cancel-button',
    });

    this.props.onCancel();
  };

  handleFieldNameChange = (event) => {
    const nextFieldName = FormsUtils.getValueFromInput(event.target);

    this.setState({ fieldName: nextFieldName });
  };

  handleConfigChange = (nextConfig) => {
    this.setState({ config: nextConfig });
  };

  handleProviderTypeChange = (nextProvider) => {
    this.props.sendTelemetry(
      nextProvider === 'lookup-v1'
        ? TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.SET_VALUE_FROM_LOOKUP_TABLE_SELECTED
        : TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.SET_VALUE_FROM_TEMPLATE_SELECTED,
      {
        app_action_value: 'set-value-from-select',
        value_source: nextProvider,
      },
    );

    const { config } = this.state;
    const providerPlugin = getProviderPlugin(nextProvider);
    const defaultProviderConfig = providerPlugin.defaultConfig || {};
    const nextConfig = {
      ...config,
      providers: [
        {
          ...defaultProviderConfig,
          type: nextProvider,
        },
      ],
    };

    this.handleConfigChange(nextConfig);
  };

  handleKeySortChange = (event) => {
    const nextPosition = event.target.value === '' ? '' : FormsUtils.getValueFromInput(event.target);

    this.setState({ keyPosition: nextPosition });
  };

  toggleKey = (event) => {
    const checked = FormsUtils.getValueFromInput(event.target);

    this.props.sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.AS_EVENT_KEY_TOGGLED, {
      app_action_value: 'event-key-checkbox',
      event_key_checked: checked,
    });

    this.setState({ isKey: checked });
  };

  renderFieldValueProviderForm = () => {
    const { fieldName, config, validation } = this.state;
    const { currentUser } = this.props;

    const providerType = getConfigProviderType(config);

    if (!providerType) {
      return null;
    }

    const providerPlugin = getProviderPlugin(providerType);

    return providerPlugin?.formComponent ? (
      React.createElement(providerPlugin.formComponent, {
        fieldName: fieldName,
        config: config,
        onChange: this.handleConfigChange,
        validation: validation,
        currentUser: currentUser,
      })
    ) : (
      <div>{t('field_form.provider_unavailable')}</div>
    );
  };

  render() {
    const { fieldName: prevFieldName } = this.props;
    const { fieldName, isKey, keyPosition, config, validation } = this.state;
    const isEditingField = !!prevFieldName;

    return (
      <Row>
        <Col md={7} lg={6}>
          <h2 className={commonStyles.title}>
            {isEditingField ? t('field_form.title_edit', { fieldName }) : t('field_form.title_new')}
          </h2>

          <Input
            id="field-name"
            name="name"
            label={t('field_form.name')}
            type="text"
            value={fieldName}
            onChange={this.handleFieldNameChange}
            bsStyle={validation.errors.fieldName ? 'error' : null}
            help={validation.errors.fieldName || t('field_form.name_help')}
            required
          />

          <FormGroup validationState={validation.errors.key_position ? 'error' : null}>
            <ControlLabel>
              {t('field_form.use_as_key')}&emsp;
              <HoverForHelp title={t('field_form.more_about_keys')} trigger={['click', 'hover']} placement="right" width={400}>
                <EventKeyHelpPopover />
              </HoverForHelp>
            </ControlLabel>
            <InputGroup>
              <InputGroup.Addon>
                <input
                  id="is-key"
                  name="is-key"
                  type="checkbox"
                  aria-label={t('field_form.use_as_key')}
                  onChange={this.toggleKey}
                  checked={isKey}
                />
              </InputGroup.Addon>
              <FormControl
                id="field-key"
                name="key"
                type="number"
                value={keyPosition}
                onChange={this.handleKeySortChange}
                disabled={!isKey}
              />
            </InputGroup>
            <HelpBlock>
              {validation.errors.key_position || t('field_form.key_help')}
            </HelpBlock>
          </FormGroup>

          <FormGroup>
            <ControlLabel>{t('field_form.data_type')}</ControlLabel>
            <FormControl.Static>{t('field_form.data_type_string')}</FormControl.Static>
          </FormGroup>

          <FormGroup
            controlId="event-field-provider"
            validationState={validation.errors['config.providers[0].type'] ? 'error' : null}>
            <ControlLabel>{t('field_form.set_value_from')}</ControlLabel>
            <Select
              name="event-field-provider"
              ignoreAccents={false}
              placeholder={t('field_form.select_value_source')}
              onChange={this.handleProviderTypeChange}
              options={formatFieldValueProviders()}
              value={getConfigProviderType(config, '')}
              required
            />
            <HelpBlock>
              {validation.errors['config.providers[0].type'] || t('field_form.value_source_help')}
            </HelpBlock>
          </FormGroup>
        </Col>

        <Col md={12}>{this.renderFieldValueProviderForm()}</Col>

        <Col md={12}>
          <ButtonToolbar>
            <Button bsStyle="primary" onClick={this.handleSubmit}>
              {isEditingField ? t('field_form.update') : t('field_form.add')}
            </Button>
            <Button onClick={this.handleCancel}>{t('common:actions.cancel')}</Button>
          </ButtonToolbar>
        </Col>
      </Row>
    );
  }
}

export default withTelemetry(FieldForm, 'event-definition-fields');
