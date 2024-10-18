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

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { BootstrapModalForm, Button, Input } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';
import { getValueFromInput } from 'util/FormsUtils';

import { PLUGIN_API_ENDPOINT, PLUGIN_CONFIG_CLASS_NAME } from '../Constants';
import UserNotification from '../../util/UserNotification';

type Props = {
  config?: {
    lookups_enabled: boolean,
    lookup_regions: string,
    access_key: string,
    secret_key: string,
    proxy_enabled: boolean,
    secret_key_salt?: string
  }
}

const _initialState = (config) => omit(config, ['secret_key', 'secret_key_salt']);

const postConfigUpdate = (update) => {
  const url = URLUtils.qualifyUrl(PLUGIN_API_ENDPOINT);

  return fetch('PUT', url, update);
};

const AWSPluginConfiguration = ({
  config = {
    lookups_enabled: false,
    lookup_regions: 'us-east-1,us-west-1,us-west-2,eu-west-1,eu-central-1',
    access_key: '',
    secret_key: '',
    proxy_enabled: false,
  },
}: Props) => {
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
    postConfigUpdate(updateConfig)
      .then(
        () => {
          ConfigurationsActions.list(PLUGIN_CONFIG_CLASS_NAME);
          closeModal();
        },
        (error) => {
          UserNotification.error(`AWS plugin configuration failed with status: ${error}`,
            'Could not save AWS plugin configuration.');
        });
  };

  return (
    <div>
      <h3>AWS Plugin Configuration</h3>

      <p>
        Base configuration for all plugins the AWS module is providing. Note
        that some parameters will be stored in MongoDB without encryption.
        Graylog users with required permissions will be able to read them in
        the configuration dialog on this page.
      </p>

      <dl className="deflist">
        <dt>Instance detail lookups:</dt>
        <dd>
          {config.lookups_enabled === true
            ? 'Enabled'
            : 'Disabled'}
        </dd>

        <dt>Connect through proxy:</dt>
        <dd>
          {config.proxy_enabled === true
            ? 'Enabled'
            : 'Disabled'}
        </dd>

        <dt>Lookup regions:</dt>
        <dd>
          {config.lookup_regions
            ? config.lookup_regions
            : '[not set]'}
        </dd>

        <dt>Access Key:</dt>
        <dd>
          {config.access_key ? config.access_key : '[not set]'}
        </dd>

        <dt>Secret Key:</dt>
        <dd>
          {config.secret_key ? '***********' : '[not set]'}
        </dd>
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={openModal}>
          Edit configuration
        </Button>
      </IfPermitted>

      <BootstrapModalForm show={showAwsConfigModal}
                          title="Update AWS Plugin Configuration"
                          onSubmitForm={saveConfig}
                          onCancel={resetConfig}
                          submitButtonText="Update configuration">
        <fieldset>
          <Input id="aws-lookups-enabled"
                 type="checkbox"
                 label="Run AWS instance detail lookups for IP addresses?"
                 help={(
                   <span>
                     When enabled, a message processor will try to identify IP
                     addresses of your AWS entities (like EC2, ELB, RDS, ...) and
                     add additional information abut the service or instance behind
                     it. It can take up to a minute for a change of this to take
                     effect.
                   </span>
                 )}
                 name="lookups_enabled"
                 checked={updateConfig.lookups_enabled}
                 onChange={onUpdate('lookups_enabled')} />

          <Input id="aws-access-key"
                 type="text"
                 label="AWS Access Key"
                 help={(
                   <span>
                     Note that this will only be used in encrypted connections but
                     stored in plaintext. Please consult the documentation for
                     suggested rights to assign to the underlying IAM user.
                   </span>
                 )}
                 name="access_key"
                 value={updateConfig.access_key}
                 onChange={onUpdate('access_key')} />

          <Input id="aws-secret-key"
                 type="password"
                 label="AWS Secret Key"
                 help={(
                   <span>
                     Note that this will only be used in encrypted connections and will be
                     stored encrypted (using the system secret). Please consult the documentation for
                     suggested rights to assign to the underlying IAM user.
                   </span>
                 )}
                 name="secret_key"
                 value={updateConfig.secret_key !== undefined ? updateConfig.secret_key : config.secret_key}
                 onFocus={onFocusSecretKey}
                 onChange={onUpdate('secret_key')} />

          <Input id="aws-lookup-regions"
                 type="text"
                 label="Lookup regions"
                 help={(
                   <span>
                     The AWS instance lookup message processor keeps a table of
                     instances for fast address translation. Define the AWS regions
                     you want to include in the tables. This should be all regions
                     you run AWS services in. Remember that your IAM user needs
                     permission for these regions or you will see warnings in your
                     graylog-server log files.
                   </span>
                 )}
                 name="lookup_regions"
                 value={updateConfig.lookup_regions}
                 onChange={onUpdate('lookup_regions')} />

          <Input id="aws-proxy-enabled"
                 type="checkbox"
                 label="Use HTTP proxy?"
                 help={(
                   <span>
                     When enabled, we&apos;ll access the AWS APIs through the HTTP proxy configured (<code>http_proxy_uri</code>)
                     in your Graylog configuration file.<br />
                     <em>Important:</em> You have to restart all AWS inputs for this configuration to take effect.
                   </span>
                 )}
                 name="proxy_enabled"
                 checked={updateConfig.proxy_enabled}
                 onChange={onUpdate('proxy_enabled')} />
        </fieldset>
      </BootstrapModalForm>
    </div>
  );
};

export default AWSPluginConfiguration;
