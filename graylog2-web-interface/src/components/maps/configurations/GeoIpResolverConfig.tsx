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
import React, { useEffect, useState } from 'react';
import { Field, Form, Formik } from 'formik';

import { IfPermitted, Select, TimeUnitInput } from 'components/common';
import { Button, Col, Input, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import { DocumentationLink } from 'components/support';

export type GeoVendorType = 'MAXMIND' | 'IPINFO'
export type TimeUnit = 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS'

export type GeoIpConfigType = {
  enabled: boolean;
  enforce_graylog_schema: boolean;
  db_vendor_type: GeoVendorType;
  city_db_path: string;
  asn_db_path: string;
  refresh_interval_unit: TimeUnit;
  refresh_interval: number;
  use_s3: boolean;
}

export type OptionType = {
  value: string;
  label: string;
}

type Props = {
  config: GeoIpConfigType,
  updateConfig: (config: GeoIpConfigType) => Promise<GeoIpConfigType>,
};

const defaultConfig: GeoIpConfigType = {
  enabled: false,
  enforce_graylog_schema: true,
  db_vendor_type: 'MAXMIND',
  city_db_path: '/etc/graylog/server/GeoLite2-City.mmdb',
  asn_db_path: '/etc/graylog/server/GeoLite2-ASN.mmdb',
  refresh_interval_unit: 'MINUTES',
  refresh_interval: 10,
  use_s3: false,
};

const GeoIpResolverConfig = ({ config = defaultConfig, updateConfig }: Props) => {
  const [showModal, setShowModal] = useState(false);
  const [curConfig, setCurConfig] = useState(() => {
    return { ...defaultConfig };
  });

  useEffect(() => {
    setCurConfig({ ...config });
  }, [config]);

  const resetConfig = () => {
    setCurConfig(config);
    setShowModal(false);
  };

  const handleSubmit = (values: GeoIpConfigType) => {
    return updateConfig(values)
      .then((value: GeoIpConfigType) => {
        if ('enabled' in value) {
          setShowModal(false);
        }
      });
  };

  const availableVendorTypes = (): OptionType[] => {
    return [
      { value: 'MAXMIND', label: 'MaxMind GeoIP' },
      { value: 'IPINFO', label: 'IPInfo Standard Location' },
    ];
  };

  const activeVendorType = (type: GeoVendorType) => {
    return availableVendorTypes().filter((t) => t.value === type)[0].label;
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
        <dd>{config.enabled === true ? 'Yes' : 'No'}</dd>
        {config.enabled && (
          <>
            <dt>Enforce default Graylog schema:</dt>
            <dd>{config.enforce_graylog_schema === true ? 'Yes' : 'No'}</dd>
            <dt>Database vendor type:</dt>
            <dd>{activeVendorType(config.db_vendor_type)}</dd>
            <dt>City database path:</dt>
            <dd>{config.city_db_path}</dd>
            <dt>ASN database path:</dt>
            <dd>{config.asn_db_path}</dd>
            <dt>Database refresh interval:</dt>
            <dd>{config.refresh_interval} {config.refresh_interval_unit}</dd>
            <dt>Pull files from S3 bucket:</dt>
            <dd>{config.use_s3 === true ? 'Yes' : 'No'}</dd>
          </>
        )}
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button bsStyle="info"
                bsSize="xs"
                onClick={() => {
                  setShowModal(true);
                }}>
          Update
        </Button>
      </IfPermitted>
      <Modal show={showModal} onHide={resetConfig} aria-modal="true" aria-labelledby="dialog_label">
        <Formik onSubmit={handleSubmit} initialValues={curConfig}>
          {({ values, setFieldValue, isSubmitting }) => {
            return (
              <Form>
                <Modal.Header>
                  <Modal.Title id="dialog_label">Update Geo-Location Processor Configuration</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                  <Row>
                    <Col sm={6}>
                      <FormikInput id="enabled"
                                   type="checkbox"
                                   label="Enable Geo-Location processor"
                                   name="enabled" />
                    </Col>
                    <Col sm={6}>
                      <FormikInput id="enforce_graylog_schema"
                                   type="checkbox"
                                   label="Enforce default Graylog schema"
                                   name="enforce_graylog_schema" />
                    </Col>
                  </Row>
                  <Field id="db_vendor_type_select"
                         name="db_vendor_type_field">
                    {() => (
                      <Input id="db_vendor_type_input"
                             label="Select the GeoIP database vendor">
                        <Select id="db_vendor_type"
                                name="db_vendor_type"
                                clearable={false}
                                placeholder="Select the GeoIP database vendor"
                                required
                                disabled={!values.enabled}
                                options={availableVendorTypes()}
                                matchProp="label"
                                value={values.db_vendor_type}
                                onChange={(option) => {
                                  setFieldValue('db_vendor_type', option);
                                }} />
                      </Input>
                    )}
                  </Field>
                  <FormikInput id="city_db_path"
                               type="text"
                               disabled={!values.enabled}
                               label="Path to the city database"
                               name="city_db_path"
                               required />
                  <FormikInput id="asn_db_path"
                               type="text"
                               disabled={!values.enabled}
                               label="Path to the ASN database"
                               name="asn_db_path" />
                  <TimeUnitInput label="Database refresh interval"
                                 update={(value, unit) => {
                                   setFieldValue('refresh_interval', value);
                                   setFieldValue('refresh_interval_unit', unit);
                                 }}
                                 help="Interval at which the database files are checked for modifications and refreshed changes are detected on disk."
                                 value={values.refresh_interval}
                                 unit={values.refresh_interval_unit || 'MINUTES'}
                                 defaultEnabled={values.enabled}
                                 enabled={values.enabled}
                                 hideCheckbox
                                 units={['SECONDS', 'MINUTES', 'HOURS', 'DAYS']} />

                  <Row>
                    <Col sm={6}>
                      <FormikInput id="use_s3"
                                   type="checkbox"
                                   label="Pull files from S3 bucket"
                                   name="use_s3" />
                    </Col>
                  </Row>
                </Modal.Body>
                <Modal.Footer>
                  <Button type="button"
                          bsStyle="link"
                          onClick={resetConfig}
                          disabled={isSubmitting}
                          aria-disabled={isSubmitting}>
                    Close
                  </Button>
                  <Button type="submit"
                          bsStyle="success"
                          disabled={isSubmitting}
                          aria-disabled={isSubmitting}>
                    {isSubmitting ? 'Saving...' : 'Save'}
                  </Button>
                </Modal.Footer>
              </Form>
            );
          }}
        </Formik>
      </Modal>
    </div>
  );
};

export default GeoIpResolverConfig;
