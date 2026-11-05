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
import React, { useState } from 'react';
import omit from 'lodash/omit';
import { Trans, useTranslation } from 'react-i18next';

import { AWSConfig } from '@graylog/server-api';

import { BootstrapModalForm, Button, Input } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { getValueFromInput } from 'util/FormsUtils';
import useProductName from 'brand-customization/useProductName';

import { PLUGIN_CONFIG_CLASS_NAME } from '../Constants';
import UserNotification from '../../util/UserNotification';

type Props = {
  config?: {
    lookups_enabled: boolean;
    lookup_regions: string;
    access_key: string;
    secret_key: string;
    proxy_enabled: boolean;
    secret_key_salt?: string;
  };
};

const _initialState = (config) => omit(config, ['secret_key', 'secret_key_salt']);

const postConfigUpdate = (update) => AWSConfig.updateConfig(update);
const AWSPluginConfiguration = ({
  config = {
    lookups_enabled: false,
    lookup_regions: 'us-east-1,us-west-1,us-west-2,eu-west-1,eu-central-1',
    access_key: '',
    secret_key: '',
    proxy_enabled: false,
  },
}: Props) => {
  const productName = useProductName();
  const { t } = useTranslation('integrations');
  const [updateConfig, setUpdateConfig] = useState(_initialState(config));
  const [showAwsConfigModal, setShowAwsConfigModal] = useState(false);

  const updateConfigField = (field, value) => {
    setUpdateConfig({ ...updateConfig, [field]: value });
  };

  const onFocusSecretKey = () => {
    setUpdateConfig({ ...updateConfig, secret_key: '' });
  };

  const onUpdate = (field) => (value) => {
    if (typeof value === 'object') {
      updateConfigField(field, getValueFromInput(value.target));
    } else {
      updateConfigField(field, value);
    }
  };

  const openModal = () => {
    setShowAwsConfigModal(true);
  };

  const closeModal = () => {
    setShowAwsConfigModal(false);
  };

  const resetConfig = () => {
    setUpdateConfig(_initialState(config));
    closeModal();
  };

  const saveConfig = () => {
    postConfigUpdate(updateConfig).then(
      () => {
        ConfigurationsActions.list(PLUGIN_CONFIG_CLASS_NAME);
        closeModal();
      },
      (error) => {
        UserNotification.error(
          t('aws_plugin_configuration.save_error_message', { error }),
          t('aws_plugin_configuration.save_error_title'),
        );
      },
    );
  };

  return (
    <div>
      <h3>{t('aws_plugin_configuration.title')}</h3>

      <p>{t('aws_plugin_configuration.description')}</p>

      <dl className="deflist">
        <dt>{t('aws_plugin_configuration.status_instance_detail_lookups')}</dt>
        <dd>
          {config.lookups_enabled === true
            ? t('aws_plugin_configuration.status_enabled')
            : t('aws_plugin_configuration.status_disabled')}
        </dd>

        <dt>{t('aws_plugin_configuration.status_connect_through_proxy')}</dt>
        <dd>
          {config.proxy_enabled === true
            ? t('aws_plugin_configuration.status_enabled')
            : t('aws_plugin_configuration.status_disabled')}
        </dd>

        <dt>{t('aws_plugin_configuration.status_lookup_regions')}</dt>
        <dd>{config.lookup_regions ? config.lookup_regions : t('aws_plugin_configuration.status_not_set')}</dd>

        <dt>{t('aws_plugin_configuration.status_access_key')}</dt>
        <dd>{config.access_key ? config.access_key : t('aws_plugin_configuration.status_not_set')}</dd>

        <dt>{t('aws_plugin_configuration.status_secret_key')}</dt>
        <dd>{config.secret_key ? '***********' : t('aws_plugin_configuration.status_not_set')}</dd>
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>
          {t('aws_plugin_configuration.edit_configuration')}
        </Button>
      </IfPermitted>

      <BootstrapModalForm
        show={showAwsConfigModal}
        title={t('aws_plugin_configuration.modal_title')}
        onSubmitForm={saveConfig}
        onCancel={resetConfig}
        submitButtonText={t('aws_plugin_configuration.submit_configuration')}>
        <fieldset>
          <Input
            id="aws-lookups-enabled"
            type="checkbox"
            label={t('aws_plugin_configuration.lookups_enabled_label')}
            help={<span>{t('aws_plugin_configuration.lookups_enabled_help')}</span>}
            name="lookups_enabled"
            checked={updateConfig.lookups_enabled}
            onChange={onUpdate('lookups_enabled')}
          />

          <Input
            id="aws-access-key"
            type="text"
            label={t('aws_plugin_configuration.access_key_label')}
            help={<span>{t('aws_plugin_configuration.access_key_help')}</span>}
            name="access_key"
            value={updateConfig.access_key}
            onChange={onUpdate('access_key')}
          />

          <Input
            id="aws-secret-key"
            type="password"
            label={t('aws_plugin_configuration.secret_key_label')}
            help={<span>{t('aws_plugin_configuration.secret_key_help')}</span>}
            name="secret_key"
            value={updateConfig.secret_key !== undefined ? updateConfig.secret_key : config.secret_key}
            onFocus={onFocusSecretKey}
            onChange={onUpdate('secret_key')}
          />

          <Input
            id="aws-lookup-regions"
            type="text"
            label={t('aws_plugin_configuration.lookup_regions_label')}
            help={<span>{t('aws_plugin_configuration.lookup_regions_help', { productName })}</span>}
            name="lookup_regions"
            value={updateConfig.lookup_regions}
            onChange={onUpdate('lookup_regions')}
          />

          <Input
            id="aws-proxy-enabled"
            type="checkbox"
            label={t('aws_plugin_configuration.proxy_enabled_label')}
            help={
              <span>
                <Trans
                  t={t}
                  i18nKey="aws_plugin_configuration.proxy_enabled_help"
                  values={{ productName }}
                  components={{ code: <code />, lineBreak: <br />, em: <em /> }}
                />
              </span>
            }
            name="proxy_enabled"
            checked={updateConfig.proxy_enabled}
            onChange={onUpdate('proxy_enabled')}
          />
        </fieldset>
      </BootstrapModalForm>
    </div>
  );
};

export default AWSPluginConfiguration;
