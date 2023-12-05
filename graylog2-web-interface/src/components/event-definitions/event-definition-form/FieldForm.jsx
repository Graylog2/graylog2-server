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
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import get from 'lodash/get';
import isNumber from 'lodash/isNumber';

import {
  OverlayTrigger,
  Icon,
  Select,
} from 'components/common';
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
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import withLocation from 'routing/withLocation';

import commonStyles from '../common/commonStyles.css';

const requiredFields = [
  'fieldName',
  'config.providers[0].type',
];

const getProviderPlugin = (type) => {
  if (type === undefined) {
    return {};
  }

  return PluginStore.exports('fieldValueProviders').find((edt) => edt.type === type) || {};
};

const getConfigProviderType = (config, defaultValue) => get(config, 'providers[0].type', defaultValue);

const formatFieldValueProviders = () => PluginStore.exports('fieldValueProviders')
  .map((type) => ({ label: type.displayName, value: type.type }));

class FieldForm extends React.Component {
  static propTypes = {
    fieldName: PropTypes.string,
    config: PropTypes.object,
    currentUser: PropTypes.object.isRequired,
    keys: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    sendTelemetry: PropTypes.func.isRequired,
    location: PropTypes.object.isRequired,
  };

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
    const errors = {};

    const providerType = getConfigProviderType(config);
    let pluginRequiredFields = [];

    if (providerType) {
      const providerPlugin = getProviderPlugin(providerType);

      pluginRequiredFields = providerPlugin.requiredFields;
    }

    requiredFields.forEach((requiredField) => {
      if (!get(this.state, requiredField)) {
        errors[requiredField] = 'Field cannot be empty.';
      }
    });

    if (isKey && (!isNumber(keyPosition) || Number(keyPosition) < 1)) {
      errors.key_position = 'Field must be a positive number.';
    }

    pluginRequiredFields.forEach((requiredField) => {
      if (!get(config, `providers[0].${requiredField}`)) {
        errors[requiredField] = 'Field cannot be empty.';
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
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_section: 'event-definition-fields',
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
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_section: 'event-definition-fields',
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
      (nextProvider === 'lookup-v1')
        ? TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.SET_VALUE_FROM_LOOKUP_TABLE_SELECTED
        : TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.SET_VALUE_FROM_TEMPLATE_SELECTED, {
        app_pathname: getPathnameWithoutId(this.props.location.pathname),
        app_section: 'event-definition-fields',
        app_action_value: 'set-value-from-select',
        value_source: nextProvider,
      });

    const { config } = this.state;
    const providerPlugin = getProviderPlugin(nextProvider);
    const defaultProviderConfig = providerPlugin.defaultConfig || {};
    const nextConfig = {
      ...config,
      providers: [{
        ...defaultProviderConfig,
        type: nextProvider,
      }],
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
      app_pathname: getPathnameWithoutId(this.props.location.pathname),
      app_section: 'event-definition-fields',
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

    return (providerPlugin.formComponent
      ? React.createElement(providerPlugin.formComponent, {
        fieldName: fieldName,
        config: config,
        onChange: this.handleConfigChange,
        validation: validation,
        currentUser: currentUser,
      })
      : <div>Selected provider is not available.</div>
    );
  };

  render() {
    const { fieldName: prevFieldName } = this.props;
    const { fieldName, isKey, keyPosition, config, validation } = this.state;

    return (
      <Row>
        <Col md={7} lg={6}>
          <h2 className={commonStyles.title}>
            {prevFieldName ? `Custom Field "${fieldName}"` : 'New Custom Field'}
          </h2>

          <Input id="field-name"
                 name="name"
                 label="Name"
                 type="text"
                 value={fieldName}
                 onChange={this.handleFieldNameChange}
                 bsStyle={validation.errors.fieldName ? 'error' : null}
                 help={validation.errors.fieldName || 'Name for this Field.'}
                 required />

          <FormGroup validationState={validation.errors.key_position ? 'error' : null}>
            <ControlLabel>
              Use Field as Event Key&emsp;
              <OverlayTrigger placement="right"
                              trigger={['click', 'focus']}
                              overlay={<EventKeyHelpPopover id="key-popover" />}>
                <Button bsStyle="link" bsSize="xsmall"><Icon name="question-circle" /></Button>
              </OverlayTrigger>
            </ControlLabel>
            <InputGroup>
              <InputGroup.Addon>
                <input id="is-key" name="is-key" type="checkbox" onChange={this.toggleKey} checked={isKey} />
              </InputGroup.Addon>
              <FormControl id="field-key"
                           name="key"
                           type="number"
                           value={keyPosition}
                           onChange={this.handleKeySortChange}
                           disabled={!isKey} />
            </InputGroup>
            <HelpBlock>
              {validation.errors.key_position || 'Indicates if this Field should be a Key and its order.'}
            </HelpBlock>
          </FormGroup>

          <FormGroup>
            <ControlLabel>Field Data Type</ControlLabel>
            <FormControl.Static>String</FormControl.Static>
          </FormGroup>

          <FormGroup controlId="event-field-provider"
                     validationState={validation.errors['config.providers[0].type'] ? 'error' : null}>
            <ControlLabel>Set Value From</ControlLabel>
            <Select name="event-field-provider"
                    ignoreAccents={false}
                    placeholder="Select Value Source"
                    onChange={this.handleProviderTypeChange}
                    options={formatFieldValueProviders()}
                    value={getConfigProviderType(config, '')}
                    matchProp="label"
                    required />
            <HelpBlock>
              {validation.errors['config.providers[0].type'] || 'Select a source for the value of this Field.'}
            </HelpBlock>
          </FormGroup>
        </Col>

        <Col md={12}>
          {this.renderFieldValueProviderForm()}
        </Col>

        <Col md={12}>
          <ButtonToolbar>
            <Button bsStyle="success" onClick={this.handleSubmit}>Add custom field</Button>
            <Button onClick={this.handleCancel}>Cancel</Button>
          </ButtonToolbar>
        </Col>
      </Row>
    );
  }
}

export default withLocation(withTelemetry(FieldForm));
