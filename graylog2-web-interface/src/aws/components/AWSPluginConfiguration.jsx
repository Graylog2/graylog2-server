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
import PropTypes from 'prop-types';
import React from 'react';

import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import { Button, BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';
import { ConfigurationsActions } from 'stores/configurations/ConfigurationsStore';

import { PLUGIN_API_ENDPOINT, PLUGIN_CONFIG_CLASS_NAME } from '../Constants';

// eslint-disable-next-line camelcase
const _initialState = ({ config, config: { secret_key, secret_key_salt, ...configWithoutSecretKey } }) => ({
  config: ObjectUtils.clone(config),
  update: configWithoutSecretKey,
  awsConfigModal: false,
});

class AWSPluginConfiguration extends React.Component {
  constructor(props) {
    super(props);

    this.state = _initialState(props);
  }

  // eslint-disable-next-line react/no-deprecated
  componentWillReceiveProps(newProps) {
    this.setState(_initialState(newProps));
  }

  _updateConfigField = (field, value) => {
    this.setState(({ update }) => ({ update: { ...update, [field]: value } }));
  };

  _onCheckboxClick = (field, ref) => {
    return () => {
      this._updateConfigField(field, this[ref].getChecked());
    };
  };

  _onFocusSecretKey = () => {
    this.setState(({ update }) => ({ update: { ...update, secret_key: '' } }));
  };

  _onSelect = (field) => {
    return (selection) => {
      this._updateConfigField(field, selection);
    };
  };

  _onUpdate = (field) => {
    return (e) => {
      this._updateConfigField(field, e.target.value);
    };
  };

  _openModal = () => {
    this.setState({ awsConfigModal: true });
  };

  _closeModal = () => {
    this.setState({ awsConfigModal: false });
  };

  _resetConfig = () => {
    // Reset to initial state when the modal is closed without saving.
    this.setState(_initialState(this.props));
  };

  _postConfigUpdate = (update) => {
    const url = URLUtils.qualifyUrl(PLUGIN_API_ENDPOINT);

    return fetch('PUT', url, update);
  };

  _saveConfig = () => {
    const { update } = this.state;

    this._postConfigUpdate(update)
      .then(() => ConfigurationsActions.list(PLUGIN_CONFIG_CLASS_NAME))
      .then(() => this._closeModal());
  };

  render() {
    const { config, update, awsConfigModal } = this.state;

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
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>
            Edit configuration
          </Button>
        </IfPermitted>

        <BootstrapModalForm show={awsConfigModal}
                            title="Update AWS Plugin Configuration"
                            onSubmitForm={this._saveConfig}
                            onCancel={this._resetConfig}
                            submitButtonText="Update configuration">
          <fieldset>
            <Input id="aws-lookups-enabled"
                   type="checkbox"
                   ref={(elem) => { this.lookupsEnabled = elem; }}
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
                   checked={update.lookups_enabled}
                   onChange={this._onCheckboxClick(
                     'lookups_enabled',
                     'lookupsEnabled',
                   )} />

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
                   value={update.access_key}
                   onChange={this._onUpdate('access_key')} />

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
                   value={update.secret_key !== undefined ? update.secret_key : config.secret_key}
                   onFocus={this._onFocusSecretKey}
                   onChange={this._onUpdate('secret_key')} />

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
                   value={update.lookup_regions}
                   onChange={this._onUpdate('lookup_regions')} />

            <Input id="aws-proxy-enabled"
                   type="checkbox"
                   ref={(elem) => { this.proxyEnabled = elem; }}
                   label="Use HTTP proxy?"
                   help={(
                     <span>
                       When enabled, we&apos;ll access the AWS APIs through the HTTP proxy configured (<code>http_proxy_uri</code>)
                       in your Graylog configuration file.<br />
                       <em>Important:</em> You have to restart all AWS inputs for this configuration to take effect.
                     </span>
)}
                   name="proxy_enabled"
                   checked={update.proxy_enabled}
                   onChange={this._onCheckboxClick('proxy_enabled', 'proxyEnabled')} />
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  }
}

AWSPluginConfiguration.propTypes = {
  // eslint-disable-next-line react/no-unused-prop-types
  config: PropTypes.object,
};

AWSPluginConfiguration.defaultProps = {
  config: {
    lookups_enabled: false,
    lookup_regions: 'us-east-1,us-west-1,us-west-2,eu-west-1,eu-central-1',
    access_key: '',
    secret_key: '',
    proxy_enabled: false,
  },
};

export default AWSPluginConfiguration;
