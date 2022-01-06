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

import { IfPermitted, Select } from 'components/common';
import { Button, BootstrapModalForm, Col, Input, Row } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import ObjectUtils from 'util/ObjectUtils';

const defaultConfig = {
  enabled: false,
  enforce_graylog_schema: true,
  db_vendor_type: 'MAXMIND',
  city_db_path: '/etc/graylog/server/GeoLite2-City.mmdb',
  asn_db_path: '/etc/graylog/server/asn.mmdb',
  run_before_extractors: false,
};

const GeoIpResolverConfig = createReactClass({
  displayName: 'GeoIpResolverConfig',

  propTypes: {
    config: PropTypes.object,
    updateConfig: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      config: {
        ...defaultConfig,
      },
    };
  },

  getInitialState() {
    const { config } = this.props;

    return {
      config: ObjectUtils.clone(config),
    };
  },

  UNSAFE_componentWillReceiveProps(newProps) {
    this.setState({ config: ObjectUtils.clone(newProps.config) });
  },

  inputs: {},

  _updateConfigField(field, value) {
    const { config } = this.state;
    let update = ObjectUtils.clone(config);

    if (field === 'enabled' && value && config.city_db_path === '' && config.asn_db_path === '') {
      update = {
        ...defaultConfig,
      };
    }

    update[field] = value;

    this.setState({ config: update });
  },

  _onCheckboxClick(field, ref) {
    return () => {
      this._updateConfigField(field, this.inputs[ref].getChecked());
    };
  },

  _onUpdate(field) {
    return (e) => {
      this._updateConfigField(field, e.target.value);
    };
  },

  _openModal() {
    this.geoIpConfigModal.open();
  },

  _closeModal() {
    this.geoIpConfigModal.close();
  },

  _resetConfig() {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.getInitialState());
  },

  _saveConfig() {
    const { updateConfig } = this.props;
    const { config } = this.state;

    const updatedConfig = { ...config };

    if (!updatedConfig.enabled) {
      updatedConfig.asn_db_path = '';
      updatedConfig.city_db_path = '';
    }

    updateConfig(updatedConfig).then(() => {
      this._closeModal();
    });
  },

  _availableDatabaseTypes() {
    return [
      { value: 'MAXMIND', label: 'MaxMind City Database' },
      { value: 'IPINFO', label: 'IPInfo Standard Location' },
    ];
  },

  _activeDatabaseType(type) {
    return this._availableDatabaseTypes().filter((t) => t.value === type)[0].label;
  },

  _onDbTypeSelect(value) {
    const { config } = this.state;
    const update = ObjectUtils.clone(config);

    update.db_vendor_type = value;
    this.setState({ config: update });
  },

  render() {
    const { config } = this.state;

    return (
      <div>
        <h3>Geo-Location Processor</h3>

        <p>
          The Geo-Location Processor plugin scans all messages for fields containing <strong>exclusively</strong> an
          IP address, and puts their geo-location information (coordinates, ISO country code, and city name) into
          different fields. Read more in the <DocumentationLink page="geolocation.html" text="Graylog documentation" />.
        </p>

        <dl className="deflist">
          <dt>Enabled:</dt>
          <dd>{config.enabled === true ? 'Yes' : 'No'}</dd>
          {config.enabled && (
            <>
              <dt>Default Graylog schema:</dt>
              <dd>{config.enforce_graylog_schema === true ? 'Yes' : 'No'}</dd>
              <dt>Database type:</dt>
              <dd>{this._activeDatabaseType(config.db_vendor_type)}</dd>
              <dt>Database path:</dt>
              <dd>{config.city_db_path}</dd>
              <dt>ASN database path:</dt>
              <dd>{config.asn_db_path}</dd>
            </>
          )}
        </dl>

        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref={(geoIpConfigModal) => { this.geoIpConfigModal = geoIpConfigModal; }}
                            title="Update Geo-Location Processor Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">
          <fieldset>
            <Row>
              <Col sm={6}>
                <Input id="geolocation-enable-checkbox"
                       type="checkbox"
                       ref={(elem) => { this.inputs.configEnabled = elem; }}
                       label="Enable Geo-Location processor"
                       name="enabled"
                       checked={config.enabled}
                       onChange={this._onCheckboxClick('enabled', 'configEnabled')} />
              </Col>
              <Col sm={6}>
                <Input id="geolocation-enforce_graylog_schema-checkbox"
                       type="checkbox"
                       ref={(elem) => { this.inputs.enforceEnabled = elem; }}
                       label="Enforce default Graylog schema"
                       name="enforce_graylog_schema"
                       checked={config.enforce_graylog_schema}
                       onChange={this._onCheckboxClick('enforce_graylog_schema', 'enforceEnabled')} />
              </Col>
            </Row>
            <Input id="maxmind-db-select"
                   label="Select the GeoIP database vendor">
              <Select placeholder="Select the GeoIP database vendor"
                      required
                      disabled={!config.enabled}
                      options={this._availableDatabaseTypes()}
                      matchProp="label"
                      value={config.db_vendor_type}
                      onChange={this._onDbTypeSelect} />
            </Input>
            <Input id="db-path"
                   type="text"
                   disabled={!config.enabled}
                   label="Path to the city database"
                   name="city_db_path"
                   value={config.city_db_path}
                   onChange={this._onUpdate('city_db_path')}
                   required />
            <Input id="asn-db-path"
                   type="text"
                   disabled={!config.enabled}
                   label="Path to the ASN database"
                   name="asn_db_path"
                   value={config.asn_db_path}
                   onChange={this._onUpdate('asn_db_path')} />
          </fieldset>
        </BootstrapModalForm>
      </div>
    );
  },
});

export default GeoIpResolverConfig;
