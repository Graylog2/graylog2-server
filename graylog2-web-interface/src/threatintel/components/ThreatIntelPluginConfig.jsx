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

import { Button } from 'components/bootstrap';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';

const ThreatIntelPluginConfig = createReactClass({
  displayName: 'ThreatIntelPluginConfig',

  propTypes: {
    config: PropTypes.object,
    updateConfig: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      config: {
        tor_enabled: false,
        spamhaus_enabled: false,
        abusech_ransom_enabled: false,
      },
    };
  },

  getInitialState() {
    const { config } = this.props;

    return {
      config: ObjectUtils.clone(config),
      threatintelConfigModal: false,
    };
  },

  componentWillReceiveProps(newProps) {
    this.setState({ config: ObjectUtils.clone(newProps.config) });
  },

  _updateConfigField(field, value) {
    const { config } = this.state;
    const update = ObjectUtils.clone(config);
    update[field] = value;
    this.setState({ config: update });
  },

  _onCheckboxClick(field, ref) {
    return () => {
      this._updateConfigField(field, this[ref].getChecked());
    };
  },

  _onSelect(field) {
    return (selection) => {
      this._updateConfigField(field, selection);
    };
  },

  _onUpdate(field) {
    return (e) => {
      this._updateConfigField(field, e.target.value);
    };
  },

  _openModal() {
    this.setState({ threatintelConfigModal: true });
  },

  _closeModal() {
    this.setState({ threatintelConfigModal: false });
  },

  _resetConfig() {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.getInitialState());
  },

  _saveConfig() {
    this.props.updateConfig(this.state.config).then(() => {
      this._closeModal();
    });
  },

  render() {
    return (
      <div>
        <h3>Threat Intelligence Lookup Configuration</h3>

        <p>
          Configuration for threat intelligence lookup plugin.
        </p>

        <dl className="deflist">
          <dt>Tor exit nodes:</dt>
          <dd>{this.state.config.tor_enabled === true ? 'Enabled' : 'Disabled'}</dd>

          <dt>Spamhaus:</dt>
          <dd>{this.state.config.spamhaus_enabled === true ? 'Enabled' : 'Disabled'}</dd>
        </dl>

        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Edit configuration</Button>
        </IfPermitted>

        <BootstrapModalForm show={this.state.threatintelConfigModal}
                            title="Update Threat Intelligence plugin Configuration"
                            onSubmitForm={this._saveConfig}
                            onCancel={this._resetConfig}
                            submitButtonText="Update configuration">
          <fieldset>
            <Input type="checkbox"
                   id="tor-checkbox"
                   ref={(elem) => { this.torEnabled = elem; }}
                   label="Allow Tor exit node lookups?"
                   help="Enable to include Tor exit node lookup in global pipeline function, disabling also stops refreshing the data."
                   name="tor_enabled"
                   checked={this.state.config.tor_enabled}
                   onChange={this._onCheckboxClick('tor_enabled', 'torEnabled')} />

            <Input type="checkbox"
                   id="spamhaus-checkbox"
                   ref={(elem) => { this.spamhausEnabled = elem; }}
                   label="Allow Spamhaus DROP/EDROP lookups?"
                   help="Enable to include Spamhaus lookup in global pipeline function, disabling also stops refreshing the data."
                   name="tor_enabled"
                   checked={this.state.config.spamhaus_enabled}
                   onChange={this._onCheckboxClick('spamhaus_enabled', 'spamhausEnabled')} />
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  },
});

export default ThreatIntelPluginConfig;
