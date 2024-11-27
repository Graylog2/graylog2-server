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
import { useState } from 'react';

import { Button, BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { SystemConfigurationComponentProps } from 'views/types';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';

type Config = {
  tor_enabled: boolean,
  spamhaus_enabled: boolean,
};
type Props = SystemConfigurationComponentProps<Config>;

const defaultConfig = {
  tor_enabled: false,
  spamhaus_enabled: false,
};

const ThreatIntelPluginConfig = ({ config: initialConfig = defaultConfig, updateConfig }: Props) => {
  const [showModal, setShowModal] = useState(false);
  const [config, setConfig] = useState<Config>(ObjectUtils.clone(initialConfig));
  const sendTelemetry = useSendTelemetry();

  const _updateConfigField = (field: string, value: boolean) => {
    const newConfig = {
      ...config,
      [field]: value,
    };
    setConfig(newConfig);
  };

  const _onCheckboxClick = (e: React.ChangeEvent<HTMLInputElement>) => {
    _updateConfigField(e.target.name, e.target.checked);
  };

  const _openModal = () => {
    setShowModal(true);
  };

  const _closeModal = () => {
    setShowModal(false);
  };

  const _resetConfig = () => {
    // Reset to initial state when the modal is closed without saving.
    setConfig(initialConfig);
  };

  const _saveConfig = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.THREATINTEL_CONFIGURATION_UPDATED, {
      app_pathname: 'configurations',
      app_section: 'threat-intel',
    });

    updateConfig(config).then(() => {
      _closeModal();
    });
  };

  return (
    <div>
      <h3>Threat Intelligence Lookup Configuration</h3>

      <p>
        Configuration for threat intelligence lookup plugin.
      </p>

      <dl className="deflist">
        <dt>Tor exit nodes:</dt>
        <dd>{config.tor_enabled === true ? 'Enabled' : 'Disabled'}</dd>

        <dt>Spamhaus:</dt>
        <dd>{config.spamhaus_enabled === true ? 'Enabled' : 'Disabled'}</dd>
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={_openModal}>Edit configuration</Button>
      </IfPermitted>

      <BootstrapModalForm show={showModal}
                          title="Update Threat Intelligence plugin Configuration"
                          onSubmitForm={_saveConfig}
                          onCancel={_resetConfig}
                          submitButtonText="Update configuration">
        <fieldset>
          <Input type="checkbox"
                 id="tor-checkbox"
                 label="Allow Tor exit node lookups?"
                 help="Enable to include Tor exit node lookup in global pipeline function, disabling also stops refreshing the data."
                 name="tor_enabled"
                 checked={config.tor_enabled}
                 onChange={_onCheckboxClick} />

          <Input type="checkbox"
                 id="spamhaus-checkbox"
                 label="Allow Spamhaus DROP/EDROP lookups?"
                 help="Enable to include Spamhaus lookup in global pipeline function, disabling also stops refreshing the data."
                 name="spamhaus_enabled"
                 checked={config.spamhaus_enabled}
                 onChange={_onCheckboxClick} />
        </fieldset>
      </BootstrapModalForm>
    </div>
  );
};

export default ThreatIntelPluginConfig;
