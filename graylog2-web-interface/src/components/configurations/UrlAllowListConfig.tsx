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
import type { AllowListConfig } from 'stores/configurations/ConfigurationsStore';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { Button, Table } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import Spinner from 'components/common/Spinner';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import UrlAllowListForm from 'components/configurations/UrlAllowListForm';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useProductName from 'brand-customization/useProductName';

const UrlAllowListConfig = () => {
  const { t } = useTranslation('configurations');
  const productName = useProductName();
  const [showConfigModal, setShowConfigModal] = useState(false);
  const configuration = useStore(ConfigurationsStore, (state) => state?.configuration);
  const [viewConfig, setViewConfig] = useState<AllowListConfig | undefined>(undefined);
  const [formConfig, setFormConfig] = useState<AllowListConfig | undefined>(undefined);
  const [isValid, setIsValid] = useState(false);

  const sendTelemetry = useSendTelemetry('urlallowlist');

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.URL_ALLOWLIST_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.URL_ALLOWLIST_CONFIG, configuration);

      setViewConfig(config);
      setFormConfig(config);
    });
  }, [configuration]);

  const summary = (): React.ReactElement<'tr'>[] => {
    const literal = 'literal';
    const { entries } = viewConfig;

    return entries.map((urlConfig, idx) => (
      <tr key={urlConfig.id}>
        <td>{idx + 1}</td>
        <td>{urlConfig.title}</td>
        <td>{urlConfig.value}</td>
        <td>{urlConfig.type === literal ? t('url_allowlist.exact_match') : t('url_allowlist.regex')}</td>
      </tr>
    ));
  };

  const openModal = () => {
    setShowConfigModal(true);
  };

  const closeModal = () => {
    setShowConfigModal(false);
    setFormConfig(viewConfig);
  };

  const saveConfig = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.URL_ALLOW_LIST_UPDATED, {
      app_action_value: 'configuration-save',
    });

    ConfigurationsActions.updateAllowlist(ConfigurationType.URL_ALLOWLIST_CONFIG, formConfig).then(() => {
      closeModal();
    });
  };

  const update = (newConfig: AllowListConfig, newIsValid: boolean) => {
    setFormConfig(newConfig);
    setIsValid(newIsValid);
  };

  if (!viewConfig || !formConfig) {
    return <Spinner />;
  }

  const { entries, disabled } = formConfig;

  return (
    <div>
      <h2>
        {t('url_allowlist.title')}{' '}
        {disabled ? <small>({t('url_allowlist.disabled')})</small> : <small>({t('url_allowlist.enabled')})</small>}
      </h2>
      <p>{t('url_allowlist.description', { productName })}</p>
      <Table bordered condensed className="top-margin">
        <thead>
          <tr>
            <th>#</th>
            <th>{t('url_allowlist.column_title')}</th>
            <th>{t('url_allowlist.column_url')}</th>
            <th>{t('url_allowlist.column_type')}</th>
          </tr>
        </thead>
        <tbody>{summary()}</tbody>
      </Table>
      <IfPermitted permissions="urlallowlist:write">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>
          {t('shared.edit_configuration')}
        </Button>
      </IfPermitted>
      {showConfigModal && (
        <BootstrapModalForm
          show
          bsSize="lg"
          title={t('url_allowlist.update_title')}
          onSubmitForm={saveConfig}
          onCancel={closeModal}
          submitButtonDisabled={!isValid}
          submitButtonText={t('shared.update_configuration')}>
          <h3>{t('url_allowlist.urls')}</h3>
          <UrlAllowListForm urls={entries} disabled={disabled} onUpdate={update} />
        </BootstrapModalForm>
      )}
    </div>
  );
};

export default UrlAllowListConfig;
