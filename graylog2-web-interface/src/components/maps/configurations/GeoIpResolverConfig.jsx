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
import React, { useEffect } from 'react';
import { useState } from 'react';

import { IfPermitted, Select } from 'components/common';
import { Button, Col, Input, Modal, Row } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import ObjectUtils from 'util/ObjectUtils';

const defaultConfig = {
  enabled: false,
  enforce_graylog_schema: true,
  db_vendor_type: 'MAXMIND',
  city_db_path: '/etc/graylog/server/GeoLite2-City.mmdb',
  asn_db_path: '/etc/graylog/server/GeoLite2-ASN.mmdb',
  run_before_extractors: false,
};

const GeoIpResolverConfig = ({ config = defaultConfig, updateConfig }) => {
  const [showModal, setShowModal] = useState(false);
  const [curConfig, setCurConfig] = useState(() => {
    return { ...defaultConfig };
  });

  useEffect(() => {
    setCurConfig({ ...config });
  }, [config]);

  const _updateConfigField = (field, value) => {
    let update = ObjectUtils.clone(curConfig);

    if (field === 'enabled' && value && curConfig.city_db_path === '' && curConfig.asn_db_path === '') {
      update = {
        ...defaultConfig,
      };
    }

    update[field] = value;

    setCurConfig(update);
  };

  const _onCheckboxClick = (setting) => {
    return () => {
      _updateConfigField(setting, !curConfig[setting]);
    };
  };

  const _onTextChange = (field) => {
    return (e) => {
      _updateConfigField(field, e.target.value);
    };
  };

  const _openModal = () => {
    setShowModal(true);
  };

  const _resetConfig = () => {
    setCurConfig(config);
    setShowModal(false);
  };

  const _handleSubmit = () => {
    updateConfig(curConfig)
      .then((value) => {
        if ('enabled' in value) {
          setShowModal(false);
        }
      });
  };

  const _availableDatabaseTypes = () => {
    return [
      { value: 'MAXMIND', label: 'MaxMind' },
      { value: 'IPINFO', label: 'IPInfo' },
    ];
  };

  const _activeDatabaseType = (type) => {
    return _availableDatabaseTypes().filter((t) => t.value === type)[0].label;
  };

  const _onDbTypeSelect = (value) => {
    const update = ObjectUtils.clone(curConfig);
    update.db_vendor_type = value;
    setCurConfig(update);
  };

  return (
    <div>
      <h3>Geo-Location Processor</h3>

      <p>
        The Geo-Location Processor plugin scans all messages for fields containing <strong>exclusively</strong> an
        IP address, and puts their geo-location information (coordinates, ISO country code, and city name) into
        different fields. Read more in the <DocumentationLink page="geolocation" text="Graylog documentation" />.
      </p>

      <dl className="deflist">
        <dt>Enabled:</dt>
        <dd>{curConfig.enabled === true ? 'Yes' : 'No'}</dd>
        {curConfig.enabled && (
          <>
            <dt>Default Graylog schema:</dt>
            <dd>{curConfig.enforce_graylog_schema === true ? 'Yes' : 'No'}</dd>
            <dt>Database vendor type:</dt>
            <dd>{_activeDatabaseType(curConfig.db_vendor_type)}</dd>
            <dt>City database path:</dt>
            <dd>{curConfig.city_db_path}</dd>
            <dt>ASN database path:</dt>
            <dd>{curConfig.asn_db_path}</dd>
          </>
        )}
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info" bsSize="xs" onClick={_openModal}>Update</Button>
      </IfPermitted>
      <Modal show={showModal} onHide={_resetConfig} aria-modal="true" aria-labelledby="dialog_label">

        <Modal.Header>
          <Modal.Title id="dialog_label">Update Geo-Location Processor Configuration</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Row>
            <Col sm={6}>
              <Input id="geolocation-enable-checkbox"
                     type="checkbox"
                     label="Enable Geo-Location processor"
                     name="enabled"
                     checked={curConfig.enabled}
                     onChange={_onCheckboxClick('enabled')} />
            </Col>
            <Col sm={6}>
              <Input id="geolocation-enforce_graylog_schema-checkbox"
                     type="checkbox"
                     label="Enforce default Graylog schema"
                     name="enforce_graylog_schema"
                     checked={curConfig.enforce_graylog_schema}
                     onChange={_onCheckboxClick('enforce_graylog_schema')} />
            </Col>
          </Row>
          <Input id="maxmind-db-select"
                 label="Select the GeoIP database vendor">
            <Select placeholder="Select the GeoIP database vendor"
                    required
                    disabled={!curConfig.enabled}
                    options={_availableDatabaseTypes()}
                    matchProp="label"
                    value={curConfig.db_vendor_type}
                    onChange={_onDbTypeSelect} />
          </Input>
          <Input id="db-path"
                 type="text"
                 disabled={!curConfig.enabled}
                 label="Path to the city database"
                 name="city_db_path"
                 value={curConfig.city_db_path}
                 onChange={_onTextChange('city_db_path')}
                 required />
          <Input id="asn-db-path"
                 type="text"
                 disabled={!curConfig.enabled}
                 label="Path to the ASN database"
                 name="asn_db_path"
                 value={curConfig.asn_db_path}
                 onChange={_onTextChange('asn_db_path')} />

        </Modal.Body>
        <Modal.Footer>
          <Button type="button" bsStyle="link" onClick={_resetConfig}>Close</Button>
          <Button type="submit" bsStyle="success" onClick={_handleSubmit}>Save</Button>
        </Modal.Footer>
      </Modal>
    </div>
  );

};

GeoIpResolverConfig.propTypes = {
  config: PropTypes.object,
  updateConfig: PropTypes.func.isRequired,
};

export default GeoIpResolverConfig;
