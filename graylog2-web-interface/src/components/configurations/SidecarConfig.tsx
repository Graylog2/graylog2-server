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
import * as React from 'react';
import { useEffect, useState } from 'react';

import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted, ISODurationInput } from 'components/common';
import ISODurationUtils from 'util/ISODurationUtils';
import { getValueFromInput } from 'util/FormsUtils';
import StringUtils from 'util/StringUtils';

type Config = {
  sidecar_expiration_threshold: string,
  sidecar_inactive_threshold: string,
  sidecar_update_interval: string,
  sidecar_send_status: boolean,
  sidecar_configuration_override: boolean,
}

const SidecarConfig = () => {
  const defaultConfig = {
    sidecar_expiration_threshold: 'P14D',
    sidecar_inactive_threshold: 'PT1M',
    sidecar_update_interval: 'PT30S',
    sidecar_send_status: true,
    sidecar_configuration_override: false,
  };

  const [showConfigModal, setShowConfigModal] = useState(false);
  const [config, setConfig] = useState<Config | undefined>(defaultConfig);

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.SIDECAR_CONFIG).then((configData) => {
      setConfig(configData as Config);
    });
  }, []);

  const openModal = () => {
    setShowConfigModal(true);
  };

  const closeModal = () => {
    setShowConfigModal(false);
  };

  const saveConfig = () => {
    ConfigurationsActions.update(ConfigurationType.SIDECAR_CONFIG, config).then(() => {
      closeModal();
    });
  };

  const onUpdate = (field) => {
    return (value) => {
      const newValue = typeof value === 'object' ? getValueFromInput(value.target) : value;

      setConfig({ ...config, [field]: newValue });
    };
  };

  const inactiveThresholdValidator = (milliseconds) => {
    return milliseconds >= 1000;
  };

  const expirationThresholdValidator = (milliseconds) => {
    return milliseconds >= 60 * 1000;
  };

  const durationMilliseconds = (duration) => {
    return ISODurationUtils.isValidDuration(duration, (milliseconds) => { return milliseconds; });
  };

  const updateIntervalValidator = (milliseconds) => {
    const inactiveMilliseconds = durationMilliseconds(config.sidecar_inactive_threshold);
    const expirationMilliseconds = durationMilliseconds(config.sidecar_expiration_threshold);

    return milliseconds >= 1000 && milliseconds < inactiveMilliseconds && milliseconds < expirationMilliseconds;
  };

  if (!config) { return null; }

  return (
    <div>
      <h2>Sidecars System</h2>

      <dl className="deflist">
        <dt>Inactive threshold:</dt>
        <dd>{config.sidecar_inactive_threshold}</dd>
        <dt>Expiration threshold:</dt>
        <dd>{config.sidecar_expiration_threshold}</dd>
        <dt>Update interval:</dt>
        <dd>{config.sidecar_update_interval}</dd>
        <dt>Send status:</dt>
        <dd>{StringUtils.capitalizeFirstLetter(config.sidecar_send_status.toString())}</dd>
        <dt>Override configuration:</dt>
        <dd>{StringUtils.capitalizeFirstLetter(config.sidecar_configuration_override.toString())}</dd>
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>Edit configuration</Button>
      </IfPermitted>

      {showConfigModal && (
      <BootstrapModalForm show
                          title="Update Sidecars System Configuration"
                          onSubmitForm={saveConfig}
                          onCancel={closeModal}
                          submitButtonText="Update configuration">
        <fieldset>
          <ISODurationInput id="inactive-threshold-field"
                            duration={config.sidecar_inactive_threshold}
                            update={onUpdate('sidecar_inactive_threshold')}
                            label="Inactive threshold (as ISO8601 Duration)"
                            help="Amount of time of inactivity after which Sidecars are flagged as inactive."
                            validator={inactiveThresholdValidator}
                            errorText="invalid (min: 1 second)"
                            required />

          <ISODurationInput id="sidecar-expiration-field"
                            duration={config.sidecar_expiration_threshold}
                            update={onUpdate('sidecar_expiration_threshold')}
                            label="Expiration threshold (as ISO8601 Duration)"
                            help="Amount of time after which inactive Sidecars are purged from the database."
                            validator={expirationThresholdValidator}
                            errorText="invalid (min: 1 minute)"
                            required />
          <ISODurationInput id="sidecar-update-field"
                            duration={config.sidecar_update_interval}
                            update={onUpdate('sidecar_update_interval')}
                            label="Update interval (as ISO8601 Duration)"
                            help="Time between Sidecar update requests."
                            validator={updateIntervalValidator}
                            errorText="invalid (min: 1 second, lower: inactive/expiration threshold)"
                            required />
        </fieldset>
        <Input type="checkbox"
               id="send-status-updates-checkbox"
               label="Send status updates"
               checked={config.sidecar_send_status}
               onChange={onUpdate('sidecar_send_status')}
               help="Send Sidecar status and host metrics from each client" />
        <Input type="checkbox"
               id="override-sidecar-config-checkbox"
               label="Override Sidecar configuration"
               checked={config.sidecar_configuration_override}
               onChange={onUpdate('sidecar_configuration_override')}
               help="Override configuration file settings for all Sidecars" />
      </BootstrapModalForm>
      )}
    </div>
  );
};

export default SidecarConfig;
