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
import lodash from 'lodash';

import { Alert, ControlLabel, FormGroup, HelpBlock } from 'components/graylog';
import { Select } from 'components/common';
import { ConfigurationFormField } from 'components/configurationforms';

import commonStyles from './LegacyNotificationCommonStyles.css';

class LegacyNotificationForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    legacyTypes: PropTypes.object.isRequired,
  };

  static defaultConfig = {
    callback_type: '',
    configuration: {},
  };

  propagateMultiChange = (newValues) => {
    const { config, onChange } = this.props;
    const nextConfig = { ...config, ...newValues };

    onChange(nextConfig);
  };

  propagateChange = (key, value) => {
    const { config } = this.props;
    const nextConfiguration = { ...config.configuration, [key]: value };

    this.propagateMultiChange({ configuration: nextConfiguration });
  };

  formatLegacyTypes = (legacyTypes) => {
    return Object.keys(legacyTypes)
      .map((typeName) => ({ label: `Legacy ${legacyTypes[typeName].name}`, value: typeName }));
  };

  getDefaultConfiguration = (legacyNotificationType) => {
    const { legacyTypes } = this.props;
    const { configuration } = legacyTypes[legacyNotificationType];
    const defaultConfiguration = {};

    Object.keys(configuration).forEach((configKey) => {
      defaultConfiguration[configKey] = configuration[configKey].default_value;
    });

    return defaultConfiguration;
  };

  handleSelectNotificationChange = (nextLegacyNotificationType) => {
    this.propagateMultiChange({
      callback_type: nextLegacyNotificationType,
      configuration: this.getDefaultConfiguration(nextLegacyNotificationType),
    });
  };

  handleFormFieldChange = (key, value) => {
    this.propagateChange(key, value);
  };

  renderNotificationForm(config, legacyType) {
    const { configuration } = legacyType;

    const configFields = Object.keys(configuration).map((configKey) => {
      const configField = configuration[configKey];
      const configValue = config.configuration[configKey];

      return (
        <ConfigurationFormField key={configKey}
                                typeName={config.callback_type}
                                configField={configField}
                                configKey={configKey}
                                configValue={configValue}
                                onChange={this.handleFormFieldChange} />
      );
    });

    return (
      <fieldset>
        {configFields}
      </fieldset>
    );
  }

  render() {
    const { config, legacyTypes, validation } = this.props;
    const callbackType = config.callback_type;
    const typeData = legacyTypes[callbackType];

    let content;

    if (typeData) {
      content = this.renderNotificationForm(config, typeData);
    } else if (callbackType) {
      content = (
        <Alert bsStyle="danger" className={commonStyles.legacyNotificationAlert}>
          Unknown legacy alarm callback type: <strong>{callbackType}</strong> Please make sure the plugin is installed.
        </Alert>
      );
    }

    return (
      <>
        <fieldset>
          <FormGroup controlId="notification-legacy-select"
                     validationState={validation.errors.callback_type ? 'error' : null}>
            <ControlLabel>Choose Legacy Notification</ControlLabel>
            <Select id="notification-legacy-select"
                    matchProp="label"
                    placeholder="Select Legacy Notification"
                    onChange={this.handleSelectNotificationChange}
                    options={this.formatLegacyTypes(legacyTypes)}
                    value={callbackType} />
            <HelpBlock>
              {lodash.get(validation, 'errors.callback_type[0]', 'Select a Legacy Notification to use on this Event Definition.')}
            </HelpBlock>
          </FormGroup>
        </fieldset>

        <Alert bsStyle="danger" className={commonStyles.legacyNotificationAlert}>
          Legacy alarm callbacks are deprecated and will be removed in Graylog 4.1. Please switch to the new notification types as soon as possible!
        </Alert>

        {content}
      </>
    );
  }
}

export default LegacyNotificationForm;
