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
import { Trans, useTranslation } from 'react-i18next';

import { useStore } from 'stores/connect';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { BootstrapModalForm, Button, Input } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import Spinner from 'components/common/Spinner';
import 'moment-duration-format';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';

type McpConfigState = {
  enable_remote_access: boolean;
  enable_output_schema: boolean;
  enable_input_validation: boolean;
};

const McpDocsLink = ({ children = undefined }: React.PropsWithChildren<{}>) => (
  <DocumentationLink text={children} page={DocsHelper.PAGES.MCP_SERVER} displayIcon={false} />
);

const McpConfig = () => {
  const { t } = useTranslation('configurations');
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);
  const [modalConfig, setModalConfig] = useState<McpConfigState | undefined>(undefined);
  const [viewConfig, setViewConfig] = useState<McpConfigState | undefined>(undefined);
  const configuration = useStore(ConfigurationsStore, (state) => state?.configuration);

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.MCP_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.MCP_CONFIG, configuration);
      setViewConfig(config);
      setModalConfig(config);
    });
  }, [configuration]);

  const openModal = () => {
    setShowConfigModal(true);
  };

  const onModalClickEnableRemoteAccess = () => {
    setModalConfig({ ...modalConfig, enable_remote_access: !modalConfig.enable_remote_access });
  };

  const onModalClickEnableOutputSchema = () => {
    setModalConfig({ ...modalConfig, enable_output_schema: !modalConfig.enable_output_schema });
  };

  const onModalClickEnableInputValidation = () => {
    setModalConfig({ ...modalConfig, enable_input_validation: !modalConfig.enable_input_validation });
  };

  const onModalCancel = () => {
    setShowConfigModal(false);
    setModalConfig(viewConfig);
  };

  const onModalSave = () => {
    ConfigurationsActions.update(ConfigurationType.MCP_CONFIG, { ...modalConfig }).then(() => {
      setShowConfigModal(false);
    });
  };

  if (!viewConfig) {
    return <Spinner />;
  }

  return (
    <div>
      <h2>{t('mcp.title')}</h2>
      <br />
      <p>{t('mcp.description')}</p>
      <p>
        <Trans t={t} i18nKey="mcp.documentation_hint" components={{ docsLink: <McpDocsLink /> }} />
      </p>
      <hr />
      <dl className="deflist">
        <dt>{t('mcp.remote_access')}</dt>
        <dd>{viewConfig.enable_remote_access ? t('markdown.enabled') : t('markdown.disabled')}</dd>
        <br />
        <dt>{t('mcp.output_schema')}</dt>
        <dd>{viewConfig.enable_output_schema ? t('markdown.enabled') : t('markdown.disabled')}</dd>
        <br />
        <dt>{t('mcp.input_validation')}</dt>
        <dd>{viewConfig.enable_input_validation ? t('markdown.enabled') : t('markdown.disabled')}</dd>
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>
          {t('shared.edit_configuration')}
        </Button>
      </IfPermitted>

      {showConfigModal && modalConfig && (
        <BootstrapModalForm
          show
          bsSize="large"
          title={t('mcp.update_title')}
          onSubmitForm={onModalSave}
          onCancel={onModalCancel}
          submitButtonText={t('shared.update_configuration')}>
          <fieldset>
            <Input
              id="enable-remote-access-checkbox"
              type="checkbox"
              label={t('mcp.enable_remote_access')}
              name="enabled"
              checked={modalConfig.enable_remote_access}
              onChange={onModalClickEnableRemoteAccess}
            />
            <Input
              id="enable-output-schema-checkbox"
              disabled={!modalConfig.enable_remote_access}
              type="checkbox"
              label={t('mcp.enable_output_schema')}
              name="output-schema-enabled"
              checked={modalConfig.enable_output_schema}
              onChange={onModalClickEnableOutputSchema}
            />
            <Input
              id="enable-input-validation-checkbox"
              disabled={!modalConfig.enable_remote_access}
              type="checkbox"
              label={t('mcp.enable_input_validation')}
              name="input-validation-enabled"
              checked={modalConfig.enable_input_validation}
              onChange={onModalClickEnableInputValidation}
            />
          </fieldset>
        </BootstrapModalForm>
      )}
    </div>
  );
};

export default McpConfig;
