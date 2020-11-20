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
// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Button, Table } from 'components/graylog';
import { IfPermitted } from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import UrlWhiteListForm from 'components/configurations/UrlWhiteListForm';
import type { WhiteListConfig } from 'stores/configurations/ConfigurationsStore';

type State = {
  config: WhiteListConfig,
  isValid: boolean,
};

type Props = {
  config: WhiteListConfig,
  updateConfig: (config: WhiteListConfig) => Promise<void>,
};

class UrlWhiteListConfig extends React.Component<Props, State> {
  configModal: ?BootstrapModalForm;

  inputs = {};

  constructor(props: Props) {
    super(props);
    const { config } = this.props;

    this.state = {
      config,
      isValid: false,
    };
  }

  _summary = (): React.Element<'tr'>[] => {
    const literal = 'literal';
    const { config: { entries } } = this.props;

    return entries.map((urlConfig, idx) => {
      return (
        <tr key={urlConfig.id}>
          <td>{idx + 1}</td>
          <td>{urlConfig.title}</td>
          <td>{urlConfig.value}</td>
          <td>{urlConfig.type === literal ? 'Exact match' : 'Regex'}</td>
        </tr>
      );
    });
  }

  _openModal = () => {
    if (this.configModal) {
      this.configModal.open();
    }
  }

  _closeModal = () => {
    if (this.configModal) {
      this.configModal.close();
    }
  }

  _saveConfig = () => {
    const { config, isValid } = this.state;
    const { updateConfig } = this.props;

    if (isValid) {
      updateConfig(config).then(() => {
        this._closeModal();
      });
    }
  }

  _update = (config: WhiteListConfig, isValid: boolean) => {
    const updatedState = { config, isValid };

    this.setState(updatedState);
  }

  _resetConfig = () => {
    const { config } = this.props;
    const updatedState = { ...this.state, config };

    this.setState(updatedState);
  }

  render() {
    const { config: { entries, disabled } } = this.props;
    const { isValid } = this.state;

    return (
      <div>
        <h2>URL Whitelist Configuration  {disabled ? <small>(Disabled)</small> : <small>(Enabled)</small> }</h2>
        <p>
          When enabled, outgoing HTTP requests from Graylog servers, such as event notifications or HTTP-based data adapter requests, are validated against the whitelists configured here.
          Because the HTTP requests are made from the Graylog servers, they might be able to reach more sensitive systems than an external user would have access to, including AWS EC2 metadata, which can contain keys and other secrets, Elasticsearch and others.
          Whitelist administrative access is separate from data adapters and event notification configuration.
        </p>
        <Table striped bordered condensed className="top-margin">
          <thead>
            <tr>
              <th>#</th>
              <th>Title</th>
              <th>URL</th>
              <th>Type</th>
            </tr>
          </thead>
          <tbody>
            {this._summary()}
          </tbody>
        </Table>
        <IfPermitted permissions="urlwhitelist:write">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>
        <BootstrapModalForm ref={(configModal) => { this.configModal = configModal; }}
                            bsSize="lg"
                            title="Update Whitelist Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonDisabled={!isValid}
                            submitButtonText="Save">
          <h3>Whitelist URLs</h3>
          <UrlWhiteListForm urls={entries} disabled={disabled} onUpdate={this._update} />
        </BootstrapModalForm>
      </div>
    );
  }
}

UrlWhiteListConfig.propTypes = {
  config: PropTypes.object.isRequired,
  updateConfig: PropTypes.func.isRequired,
};

export default UrlWhiteListConfig;
