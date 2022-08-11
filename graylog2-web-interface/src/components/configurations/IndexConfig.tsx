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
import PropTypes from 'prop-types';

import {Button} from 'components/bootstrap';
import {IfPermitted} from 'components/common';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import type {WhiteListConfig} from 'stores/configurations/ConfigurationsStore';
import IndexConfigForm from 'components/configurations/IndexConfigForm';

type State = {
  config: WhiteListConfig,
  isValid: boolean,
};

type Props = {
  config: WhiteListConfig,
  updateConfig: (config: WhiteListConfig) => Promise<void>,
};

class IndexConfig extends React.Component<Props, State> {
  private configModal: BootstrapModalForm | undefined | null;

  static propTypes = {
    config: PropTypes.object.isRequired,
    updateConfig: PropTypes.func.isRequired,
  };

  constructor(props: Props) {
    super(props);
    const {config} = this.props;

    this.state = {
      config,
      isValid: false,
    };
  }

  _openModal = () => {
    if (this.configModal) {
      this.configModal.open();
    }
  };

  _closeModal = () => {
    if (this.configModal) {
      this.configModal.close();
    }
  };

  _saveConfig = () => {
    const {config, isValid} = this.state;
    const {updateConfig} = this.props;

    if (isValid) {
      updateConfig(config).then(() => {
        this._closeModal();
      });
    }
  };

  _update = (config: WhiteListConfig, isValid: boolean) => {
    const updatedState = {config, isValid};

    this.setState(updatedState);
  };

  _resetConfig = () => {
    const {config} = this.props;
    const updatedState = {...this.state, config};

    this.setState(updatedState);
  };

  render() {
    const {config} = this.props;
    const {isValid} = this.state;

    console.log(config);
    return (
      <div>
        <h2>Index Defaults</h2>
        <p>Defaults for newly created index sets.</p>

        <dl className="deflist">
          <dt>Index prefix:</dt>
          <dd>{config.index_prefix == "" ? "<none>" : config.index_prefix }</dd>
          <dt>Index analyzer:</dt>
          <dd>{config.index_analyzer}</dd>
          <dt>Shards per Index:</dt>
          <dd>{config.shards}</dd>
          <dt>Replicas per Index:</dt>
          <dd>{config.replicas}</dd>
          <dt>Max. Number of Segments:</dt>
          <dd>{config.index_optimization_max_num_segments}</dd>
          <dt>Index optimization disabled:</dt>
          <dd>{config.index_optimization_disabled ? "Yes" : "No"}</dd>
          <dt>Field type refresh interval:</dt>
          <dd>{config.field_type_refresh_interval}</dd>
        </dl>

        <br/>
        <br/>
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
          <IndexConfigForm config={config} onUpdate={this._update} />
        </BootstrapModalForm>
      </div>
    );
  }
}

export default IndexConfig;
