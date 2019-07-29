import React from 'react';
import PropTypes from 'prop-types';
import { Alert, ControlLabel, FormGroup, HelpBlock } from 'react-bootstrap';

import { Select } from 'components/common';
import { ConfigurationFormField } from 'components/configurationforms';

import styles from './LegacyNotificationForm.css';

class LegacyNotificationForm extends React.Component {
  static propTypes = {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    legacyTypes: PropTypes.object.isRequired,
  };

  propagateMultiChange = (newValues) => {
    const { config, onChange } = this.props;
    const nextConfig = Object.assign({}, config, newValues);
    onChange(nextConfig);
  };

  propagateChange = (key, value) => {
    const { config } = this.props;
    const nextConfiguration = Object.assign({}, config.configuration, { [key]: value });
    this.propagateMultiChange({ configuration: nextConfiguration });
  };

  formatLegacyTypes = (legacyTypes) => {
    return Object.keys(legacyTypes)
      .map(typeName => ({ label: `Legacy ${legacyTypes[typeName].name}`, value: typeName }));
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

  renderMissingPlugin(pluginType) {
    return (
      <Alert bsStyle="danger" className={styles.legacyNotificationAlert}>
        Unknown legacy alarm callback type: <strong>{pluginType}</strong> Please make sure the plugin is installed.
      </Alert>
    );
  }

  render() {
    const { config, legacyTypes } = this.props;
    const callbackType = config.callback_type;
    const typeData = legacyTypes[callbackType];

    return (
      <React.Fragment>
        <fieldset>
          <FormGroup controlId="notification-legacy-select">
            <ControlLabel>Choose Legacy Notification</ControlLabel>
            <Select id="notification-legacy-select"
                    matchProp="label"
                    placeholder="Select Legacy Notification"
                    onChange={this.handleSelectNotificationChange}
                    options={this.formatLegacyTypes(legacyTypes)}
                    value={callbackType} />
            <HelpBlock>Select a Legacy Notification to use on this Event Definition.</HelpBlock>
          </FormGroup>
        </fieldset>

        <Alert bsStyle="danger" className={styles.legacyNotificationAlert}>
          Legacy alarm callbacks are deprecated. Please switch to the new notification types as soon as possible!
        </Alert>

        {typeData ? this.renderNotificationForm(config, typeData) : this.renderMissingPlugin(callbackType)}
      </React.Fragment>
    );
  }
}

export default LegacyNotificationForm;
