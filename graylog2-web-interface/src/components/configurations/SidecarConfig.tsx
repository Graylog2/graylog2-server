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
import { useTranslation } from 'react-i18next';

import { useStore } from 'stores/connect';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted, ISODurationInput } from 'components/common';
import Spinner from 'components/common/Spinner';
import * as ISODurationUtils from 'util/ISODurationUtils';
import { getValueFromInput } from 'util/FormsUtils';

type Config = {
  sidecar_expiration_threshold: string;
  sidecar_inactive_threshold: string;
  sidecar_update_interval: string;
  sidecar_send_status: boolean;
  sidecar_configuration_override: boolean;
};

const DEFAULT_CONFIG = {
  sidecar_expiration_threshold: 'P14D',
  sidecar_inactive_threshold: 'PT1M',
  sidecar_update_interval: 'PT30S',
  sidecar_send_status: true,
  sidecar_configuration_override: false,
};

const SidecarConfig = () => {
  const { t } = useTranslation('configurations');
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [viewConfig, setViewConfig] = useState<Config>(DEFAULT_CONFIG);
  const [formConfig, setFormConfig] = useState<Config>(DEFAULT_CONFIG);
  const configuration = useStore(ConfigurationsStore, (state) => state?.configuration);
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

  const durationMilliseconds = (duration: string) =>
    ISODurationUtils.isValidDuration(duration, (milliseconds) => milliseconds);

  const updateIntervalValidator = (milliseconds: number) => {
    const inactiveMilliseconds = durationMilliseconds(formConfig.sidecar_inactive_threshold);
    const expirationMilliseconds = durationMilliseconds(formConfig.sidecar_expiration_threshold);

    return milliseconds >= 1000 && milliseconds < inactiveMilliseconds && milliseconds < expirationMilliseconds;
  };

  if (!loaded || !viewConfig) {
    return <Spinner />;
  }

  return (
    <div>
      <h2>{t('sidecar.title')}</h2>

      <dl className="deflist">
        <dt>{t('sidecar.inactive_threshold')}:</dt>
        <dd>{viewConfig.sidecar_inactive_threshold}</dd>
        <dt>{t('sidecar.expiration_threshold')}:</dt>
        <dd>{viewConfig.sidecar_expiration_threshold}</dd>
        <dt>{t('sidecar.update_interval')}:</dt>
        <dd>{viewConfig.sidecar_update_interval}</dd>
        <dt>{t('sidecar.send_status')}:</dt>
        <dd>{viewConfig.sidecar_send_status ? t('shared.true') : t('shared.false')}</dd>
        <dt>{t('sidecar.override_configuration')}:</dt>
        <dd>{viewConfig.sidecar_configuration_override ? t('shared.true') : t('shared.false')}</dd>
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>
          {t('shared.edit_configuration')}
        </Button>
      </IfPermitted>

      {showConfigModal && formConfig && (
        <BootstrapModalForm
          show
          title={t('sidecar.update_title')}
          onSubmitForm={saveConfig}
          onCancel={closeModal}
          submitButtonText={t('shared.update_configuration')}>
          <fieldset>
            <ISODurationInput
              id="inactive-threshold-field"
              duration={formConfig.sidecar_inactive_threshold}
              update={onUpdate('sidecar_inactive_threshold')}
              label={t('sidecar.inactive_threshold_label')}
              help={t('sidecar.inactive_threshold_help')}
              validator={inactiveThresholdValidator}
              errorText={t('sidecar.inactive_threshold_error')}
              required
            />

            <ISODurationInput
              id="sidecar-expiration-field"
              duration={formConfig.sidecar_expiration_threshold}
              update={onUpdate('sidecar_expiration_threshold')}
              label={t('sidecar.expiration_threshold_label')}
              help={t('sidecar.expiration_threshold_help')}
              validator={expirationThresholdValidator}
              errorText={t('sidecar.expiration_threshold_error')}
              required
            />
            <ISODurationInput
              id="sidecar-update-field"
              duration={formConfig.sidecar_update_interval}
              update={onUpdate('sidecar_update_interval')}
              label={t('sidecar.update_interval_label')}
              help={t('sidecar.update_interval_help')}
              validator={updateIntervalValidator}
              errorText={t('sidecar.update_interval_error')}
              required
            />
          </fieldset>
          <Input
            type="checkbox"
            id="send-status-updates-checkbox"
            label={t('sidecar.send_status_updates')}
            checked={formConfig.sidecar_send_status}
            onChange={onUpdate('sidecar_send_status')}
            help={t('sidecar.send_status_updates_help')}
          />
          <Input
            type="checkbox"
            id="override-sidecar-config-checkbox"
            label={t('sidecar.override_sidecar_configuration')}
            checked={formConfig.sidecar_configuration_override}
            onChange={onUpdate('sidecar_configuration_override')}
            help={t('sidecar.override_sidecar_configuration_help')}
          />
        </BootstrapModalForm>
      )}
    </div>
  );
};

export default SidecarConfig;
