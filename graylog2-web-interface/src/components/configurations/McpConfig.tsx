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
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';
import { getConfig } from 'components/configurations/helpers';
import { ConfigurationType } from 'components/configurations/ConfigurationTypes';
import { BootstrapModalForm, Button, Input } from 'components/bootstrap';
import { IfPermitted, ISODurationInput } from 'components/common';
import Spinner from 'components/common/Spinner';
import 'moment-duration-format';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import useProductName from 'brand-customization/useProductName';

type McpConfigState = {
  enable_remote_access: boolean;
  enable_output_schema: boolean;
  enable_input_validation: boolean;
  enable_oauth: boolean;
  oauth_access_token_ttl: string;
  oauth_refresh_token_ttl: string;
  oauth_allowed_redirect_uris: Array<string>;
};

const parseRedirectUris = (value: string) =>
  value
    .split('\n')
    .map((uri) => uri.trim())
    .filter((uri) => uri !== '');

const McpConfig = () => {
  const [showConfigModal, setShowConfigModal] = useState<boolean>(false);
  const [modalConfig, setModalConfig] = useState<McpConfigState | undefined>(undefined);
  const [viewConfig, setViewConfig] = useState<McpConfigState | undefined>(undefined);
  const [redirectUrisText, setRedirectUrisText] = useState<string>('');
  const productName = useProductName();
  const configuration = useStore(ConfigurationsStore, (state) => state?.configuration);

  useEffect(() => {
    ConfigurationsActions.list(ConfigurationType.MCP_CONFIG).then(() => {
      const config = getConfig(ConfigurationType.MCP_CONFIG, configuration);
      setViewConfig(config);
      setModalConfig(config);
    });
  }, [configuration]);

  const openModal = () => {
    setRedirectUrisText(modalConfig.oauth_allowed_redirect_uris.join('\n'));
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

  const onModalClickEnableOAuth = () => {
    setModalConfig({ ...modalConfig, enable_oauth: !modalConfig.enable_oauth });
  };

  const onModalChangeRedirectUris = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRedirectUrisText(event.target.value);
  };

  const onModalCancel = () => {
    setShowConfigModal(false);
    setModalConfig(viewConfig);
  };

  const onModalSave = () => {
    ConfigurationsActions.update(ConfigurationType.MCP_CONFIG, {
      ...modalConfig,
      oauth_allowed_redirect_uris: parseRedirectUris(redirectUrisText),
    }).then(() => {
      setShowConfigModal(false);
    });
  };

  if (!viewConfig) {
    return <Spinner />;
  }

  return (
    <div>
      <h2>MCP Server Configuration</h2>
      <br />
      <p>Activate MCP (Model Context Protocol) to enable LLM-powered communication and automation with your cluster.</p>
      <p>
        See the{' '}
        <DocumentationLink text="MCP connection documentation" page={DocsHelper.PAGES.MCP_SERVER} displayIcon={false} />{' '}
        for client setup instructions.
      </p>
      <hr />
      <dl className="deflist">
        <dt>Remote MCP access</dt>
        <dd>{viewConfig.enable_remote_access ? 'Enabled' : 'Disabled'}</dd>
        <br />
        <dt>Output schema</dt>
        <dd>{viewConfig.enable_output_schema ? 'Enabled' : 'Disabled'}</dd>
        <br />
        <dt>Input validation</dt>
        <dd>{viewConfig.enable_input_validation ? 'Enabled' : 'Disabled'}</dd>
        <br />
        <dt>OAuth client sign-in</dt>
        <dd>{viewConfig.enable_oauth ? 'Enabled' : 'Disabled'}</dd>
        <dt>Access token lifetime</dt>
        <dd>{viewConfig.oauth_access_token_ttl}</dd>
        <dt>Refresh token lifetime</dt>
        <dd>{viewConfig.oauth_refresh_token_ttl}</dd>
        <dt>Allowed redirect URIs</dt>
        <dd>
          {viewConfig.oauth_allowed_redirect_uris?.length ? viewConfig.oauth_allowed_redirect_uris.join(', ') : '-'}
        </dd>
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>
          Edit configuration
        </Button>
      </IfPermitted>

      {showConfigModal && modalConfig && (
        <BootstrapModalForm
          show
          bsSize="large"
          title="Update MCP Server Configuration"
          onSubmitForm={onModalSave}
          onCancel={onModalCancel}
          submitButtonText="Update configuration">
          <fieldset>
            <Input
              id="enable-remote-access-checkbox"
              type="checkbox"
              label="Enable remote MCP access"
              name="enabled"
              checked={modalConfig.enable_remote_access}
              onChange={onModalClickEnableRemoteAccess}
            />
            <Input
              id="enable-output-schema-checkbox"
              disabled={!modalConfig.enable_remote_access}
              type="checkbox"
              label="Enable Output Schema generation"
              name="output-schema-enabled"
              checked={modalConfig.enable_output_schema}
              onChange={onModalClickEnableOutputSchema}
            />
            <Input
              id="enable-input-validation-checkbox"
              disabled={!modalConfig.enable_remote_access}
              type="checkbox"
              label="Enable Tool Input Validation"
              name="input-validation-enabled"
              checked={modalConfig.enable_input_validation}
              onChange={onModalClickEnableInputValidation}
            />
            <Input
              id="enable-oauth-checkbox"
              disabled={!modalConfig.enable_remote_access}
              type="checkbox"
              label="Enable OAuth client sign-in"
              name="oauth-enabled"
              help={`MCP clients only need the server URL and let users sign in through ${productName} in their browser.`}
              checked={modalConfig.enable_oauth}
              onChange={onModalClickEnableOAuth}
            />
            <ISODurationInput
              id="oauth-access-token-ttl"
              label="Access token lifetime"
              help="How long an access token issued to an MCP client is valid. Clients refresh it automatically."
              duration={modalConfig.oauth_access_token_ttl}
              update={(value) => setModalConfig({ ...modalConfig, oauth_access_token_ttl: value })}
              disabled={!modalConfig.enable_remote_access || !modalConfig.enable_oauth}
              required
            />
            <ISODurationInput
              id="oauth-refresh-token-ttl"
              label="Refresh token lifetime"
              help="After this period without use, the user has to authorize the client again."
              duration={modalConfig.oauth_refresh_token_ttl}
              update={(value) => setModalConfig({ ...modalConfig, oauth_refresh_token_ttl: value })}
              disabled={!modalConfig.enable_remote_access || !modalConfig.enable_oauth}
              required
            />
            <Input
              id="oauth-allowed-redirect-uris"
              type="textarea"
              rows={4}
              label="Allowed redirect URIs"
              help="One URI per line. Clients may use these web callbacks in addition to local (loopback) and app callbacks."
              value={redirectUrisText}
              onChange={onModalChangeRedirectUris}
              disabled={!modalConfig.enable_remote_access || !modalConfig.enable_oauth}
            />
          </fieldset>
        </BootstrapModalForm>
      )}
    </div>
  );
};

export default McpConfig;
