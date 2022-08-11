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
import createReactClass from 'create-react-class';
import lodash from 'lodash';
import moment from 'moment';

import { BootstrapModalForm, Button, FormGroup, HelpBlock } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import FormUtils from 'util/FormsUtils';
import Input from 'components/bootstrap/Input';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';

const IndexSetsDefaultsConfig = createReactClass({
  displayName: 'IndexSetsDefaultsConfig',

  propTypes: {
    config: PropTypes.shape({
      shards: PropTypes.number,
      replicas: PropTypes.number,
    }),
    updateConfig: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      config: {
        shards: 4,
        replicas: 0,
      },
    };
  },

  getInitialState() {
    const { config } = this.props;

    return {
      config: config,
    };
  },

  UNSAFE_componentWillReceiveProps(newProps) {
    this.setState({ config: newProps.config });
  },

  _openModal() {
    this.modal.open();
  },

  _closeModal() {
    this.modal.close();
  },

  _resetConfig() {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.getInitialState());
  },

  _saveConfig() {
    const { updateConfig } = this.props;
    const { config } = this.state;

    updateConfig(config).then(() => {
      this._closeModal();
    });
  },

  _propagateChanges(key, value) {
    const { config } = this.state;
    const nextConfig = lodash.cloneDeep(config);

    nextConfig[key] = value;
    this.setState({ config: nextConfig });
  },

  _onShardsUpdate(event) {
    const shards = FormUtils.getValueFromInput(event.target);
    if (this._nonNegativeIntValidator(shards)) {
      this._propagateChanges('shards', shards);
    }
  },

  _onReplicasUpdate(event) {
    const replicas = FormUtils.getValueFromInput(event.target);
    if (this._nonNegativeIntValidator(replicas)) {
      this._propagateChanges('replicas', replicas);
    }
  },

  _nonNegativeIntValidator(value) {
    return value >= 0;
  },

  render() {
    const { config } = this.state;
    return (
      <div>
        <h2>Index Defaults</h2>
        <p>Defaults for newly created index sets.</p>
        <dl className="deflist">
          <dt>Index prefix:</dt>
          <dd>{config.indexPrefix}</dd>
          <dt>Index analyzer:</dt>
          <dd>{config.indexAnalyzer}</dd>
          <dt>Shards per Index:</dt>
          <dd>{config.shards}</dd>
          <dt>Replicas per Index:</dt>
          <dd>{config.replicas}</dd>
          <dt>Max. Number of Segments:</dt>
          <dd>{config.indexOptimizationMaxNumSegments}</dd>
          <dt>Index optimization disabled:</dt>
          <dd>{config.indexOptimizationDisabled}</dd>
          <dt>Field type refresh interval</dt>
          <dd>{config.fieldTypeRefreshInterval}</dd>
        </dl>

        <p>
          <IfPermitted permissions="clusterconfigentry:edit">
            <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
          </IfPermitted>
        </p>
        <BootstrapModalForm ref={(modal) => {
          this.modal = modal;
        }}
                            title="Update Index Sets Default Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">
          <fieldset>
            <p>These defaults will apply for newly created index sets.</p>
            <Input id="index-prefix-field"
                   type="text"
                   onChange={}
                   label="Index Prefix"
                   help=""
                   value={config.indexPrefix} />
            <Input id="index-analyzer-field"
                   type="text"
                   onChange={}
                   label="Analyzer"
                   help=""
                   value={config.indexAnalyzer} />
            <Input id="shards-field"
                   type="number"
                   onChange={this._onShardsUpdate}
                   label="Index shards"
                   help=""
                   value={config.shards}
                   min="0" />
            <Input id="replicas-field"
                   type="number"
                   onChange={this._onReplicasUpdate}
                   label="Index replicas"
                   help=""
                   value={config.replicas}
                   min="0" />
            <Input id="max-segments-field"
                   type="number"
                   onChange={this._onReplicasUpdate}
                   label="Max. number of segments"
                   help=""
                   value={config.indexOptimizationMaxNumSegments}
                   min="1" />
            <label htmlFor="index-optimization-disabled-checkbox">Relative Timerange Options</label>
            <Input id="index-optimization-disabled-checkbox"
                   type="checkbox"
                   label="Disable index optimization after rotation"
                   name="enabled"
                   checked={config.indexOptimizationDisabled}
                   onChange={this._onChecked} />
            min="1" />
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  },
});

export default IndexSetsDefaultsConfig;
