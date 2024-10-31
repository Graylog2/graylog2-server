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

import { useStore } from 'stores/connect';
import type { Store } from 'stores/StoreTypes';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted, ISODurationInput } from 'components/common';
import Spinner from 'components/common/Spinner';
import * as ISODurationUtils from 'util/ISODurationUtils';
import { getValueFromInput } from 'util/FormsUtils';
import StringUtils from 'util/StringUtils';

type Config = {
  sidecar_expiration_threshold: string,
  sidecar_inactive_threshold: string,
  sidecar_update_interval: string,
  sidecar_send_status: boolean,
  sidecar_configuration_override: boolean,
}

const DEFAULT_CONFIG = {
  sidecar_expiration_threshold: 'P14D',
  sidecar_inactive_threshold: 'PT1M',
  sidecar_update_interval: 'PT30S',
  sidecar_send_status: true,
  sidecar_configuration_override: false,
};

const SidecarConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [viewConfig, setViewConfig] = useState<Config>(DEFAULT_CONFIG);
  const [formConfig, setFormConfig] = useState<Config>(DEFAULT_CONFIG);
  const configuration = useStore(ConfigurationsStore as Store<Record<string, any>>, (state) => state?.configuration);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.SIDECAR_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.SIDECAR_CONFIG, configuration);

      setViewConfig(config);
      setFormConfig(config);
      setLoaded(true);
    });
  }, [configuration]);

  const openModal = () => {
    setShowConfigModal(true);
  };

  const closeModal = () => {
    setShowConfigModal(false);
    setFormConfig(viewConfig);
  };

  const saveConfig = () => {
    ConfigurationsActions.update(ConfigurationType.SIDECAR_CONFIG, formConfig).then(() => {
      closeModal();
    });
  };

  const onUpdate = (field: string) => (value: string | React.ChangeEvent<HTMLInputElement>) => {
    const newValue = typeof value === 'object' ? getValueFromInput(value.target) : value;

    setFormConfig({ ...formConfig, [field]: newValue });
  };

  const inactiveThresholdValidator = (milliseconds: number) => milliseconds >= 1000;

  const expirationThresholdValidator = (milliseconds: number) => milliseconds >= 60 * 1000;

  const durationMilliseconds = (duration: string) => ISODurationUtils.isValidDuration(duration, (milliseconds) => milliseconds);

  const updateIntervalValidator = (milliseconds: number) => {
    const inactiveMilliseconds = durationMilliseconds(formConfig.sidecar_inactive_threshold);
    const expirationMilliseconds = durationMilliseconds(formConfig.sidecar_expiration_threshold);

    return milliseconds >= 1000 && milliseconds < inactiveMilliseconds && milliseconds < expirationMilliseconds;
  };

  if (!loaded || !viewConfig) { return <Spinner />; }

  return (
    <div>
      <h2>Sidecars Configuration</h2>

      <dl className="deflist">
        <dt>Inactive threshold:</dt>
        <dd>{viewConfig.sidecar_inactive_threshold}</dd>
        <dt>Expiration threshold:</dt>
        <dd>{viewConfig.sidecar_expiration_threshold}</dd>
        <dt>Update interval:</dt>
        <dd>{viewConfig.sidecar_update_interval}</dd>
        <dt>Send status:</dt>
        <dd>{StringUtils.capitalizeFirstLetter(viewConfig.sidecar_send_status.toString())}</dd>
        <dt>Override configuration:</dt>
        <dd>{StringUtils.capitalizeFirstLetter(viewConfig.sidecar_configuration_override.toString())}</dd>
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>Edit configuration</Button>
      </IfPermitted>

      {showConfigModal && formConfig && (
      <BootstrapModalForm show
                          title="Update Sidecars System Configuration"
                          onSubmitForm={saveConfig}
                          onCancel={closeModal}
                          submitButtonText="Update configuration">
        <fieldset>
          <ISODurationInput id="inactive-threshold-field"
                            duration={formConfig.sidecar_inactive_threshold}
                            update={onUpdate('sidecar_inactive_threshold')}
                            label="Inactive threshold (as ISO8601 Duration)"
                            help="Amount of time of inactivity after which Sidecars are flagged as inactive."
                            validator={inactiveThresholdValidator}
                            errorText="invalid (min: 1 second)"
                            required />

          <ISODurationInput id="sidecar-expiration-field"
                            duration={formConfig.sidecar_expiration_threshold}
                            update={onUpdate('sidecar_expiration_threshold')}
                            label="Expiration threshold (as ISO8601 Duration)"
                            help="Amount of time after which inactive Sidecars are purged from the database."
                            validator={expirationThresholdValidator}
                            errorText="invalid (min: 1 minute)"
                            required />
          <ISODurationInput id="sidecar-update-field"
                            duration={formConfig.sidecar_update_interval}
                            update={onUpdate('sidecar_update_interval')}
                            label="Update interval (as ISO8601 Duration)"
                            help="Time between Sidecar update requests."
                            validator={updateIntervalValidator}
                            errorText="invalid (min: 1 second, but less than Inactive threshold)"
                            required />
        </fieldset>
        <Input type="checkbox"
               id="send-status-updates-checkbox"
               label="Send status updates"
               checked={formConfig.sidecar_send_status}
               onChange={onUpdate('sidecar_send_status')}
               help="Send Sidecar status and host metrics from each client" />
        <Input type="checkbox"
               id="override-sidecar-config-checkbox"
               label="Override Sidecar configuration"
               checked={formConfig.sidecar_configuration_override}
               onChange={onUpdate('sidecar_configuration_override')}
               help="Override configuration file settings for all Sidecars" />
      </BootstrapModalForm>
      )}
    </div>
  );
};

export default SidecarConfig;
