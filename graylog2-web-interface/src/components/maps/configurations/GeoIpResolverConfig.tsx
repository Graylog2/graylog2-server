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

import { IfPermitted, Select, TimeUnitInput, ModalSubmit, InputOptionalInfo } from 'components/common';
import { Button, Col, Input, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import { DocumentationLink } from 'components/support';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

export type GeoVendorType = 'MAXMIND' | 'IPINFO';
export type TimeUnit = 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS';

const CLOUD_STORAGE_OPTION = {
  GCS: 'gcs',
  S3: 's3',
} as const;

export type GeoIpConfigType = {
  enabled: boolean;
  enforce_graylog_schema: boolean;
  db_vendor_type: GeoVendorType;
  city_db_path: string;
  asn_db_path: string;
  refresh_interval_unit: TimeUnit;
  refresh_interval: number;
  pull_from_cloud?: (typeof CLOUD_STORAGE_OPTION)[keyof typeof CLOUD_STORAGE_OPTION];
  gcs_project_id?: string;
};

export type OptionType = {
  value: string;
  label: string;
};

type Props = {
  config?: GeoIpConfigType;
  updateConfig: (config: GeoIpConfigType) => Promise<GeoIpConfigType>;
};

const defaultConfig: GeoIpConfigType = {
  enabled: false,
  enforce_graylog_schema: true,
  db_vendor_type: 'MAXMIND',
  city_db_path: '/etc/server/GeoLite2-City.mmdb',
  asn_db_path: '/etc/server/GeoLite2-ASN.mmdb',
  refresh_interval_unit: 'MINUTES',
  refresh_interval: 10,
  pull_from_cloud: undefined,
  gcs_project_id: undefined,
};

const GeoIpResolverConfig = ({ config, updateConfig }: Props) => {
  const [showModal, setShowModal] = useState(false);
  const [curConfig, setCurConfig] = useState(config);

  const sendTelemetry = useSendTelemetry();

  useEffect(() => {
    setCurConfig({ ...config });
  }, [config]);

  const resetConfig = () => {
    setCurConfig(config);
    setShowModal(false);
  };

  const handleSubmit = (values: GeoIpConfigType) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.GEOLOCATION_CONFIGURATION_UPDATED, {
      app_pathname: 'configurations',
      app_section: 'geolocation-processor',
    });

    return updateConfig(values).then((value: GeoIpConfigType) => {
      if ('enabled' in value) {
        setShowModal(false);
      }
    });
  };

  const availableVendorTypes = (): OptionType[] => [
    { value: 'MAXMIND', label: 'MaxMind GeoIP' },
    { value: 'IPINFO', label: 'IPInfo Standard Location' },
  ];

  const cloudStorageOptions: OptionType[] = [
    { value: CLOUD_STORAGE_OPTION.S3, label: 'S3' },
    { value: CLOUD_STORAGE_OPTION.GCS, label: 'Google Cloud Storage' },
  ];

  const activeVendorType = (type: GeoVendorType) => availableVendorTypes().filter((t) => t.value === type)[0].label;

  const modalTitle = 'Update Geo-Location Processor Configuration';

  return (
    <div>
      <h3>Geo-Location Processor Configuration</h3>

      <p>
        The Geo-Location Processor plugin scans all messages for fields containing <strong>exclusively</strong> an IP
        address, and puts their geo-location information (coordinates, ISO country code, and city name) into different
        fields. Read more in the <DocumentationLink page="geolocation" text="documentation" />.
      </p>

      <dl className="deflist">
        <dt>Enabled:</dt>
        <dd>{config.enabled === true ? 'Yes' : 'No'}</dd>
        {config.enabled && (
          <>
            <dt>Enforce default schema:</dt>
            <dd>{config.enforce_graylog_schema === true ? 'Yes' : 'No'}</dd>
            <dt>Database vendor type:</dt>
            <dd>{activeVendorType(config.db_vendor_type)}</dd>
            <dt>City database path:</dt>
            <dd>{config.city_db_path}</dd>
            <dt>ASN database path:</dt>
            <dd>{config.asn_db_path === '' ? '-' : config.asn_db_path}</dd>
            <dt>Database refresh interval:</dt>
            <dd>
              {config.refresh_interval} {config.refresh_interval_unit}
            </dd>
            <dt>Pull files from cloud storage bucket:</dt>
            <dd>
              {config.pull_from_cloud
                ? cloudStorageOptions.find((option) => option.value === config.pull_from_cloud)?.label
                : 'No'}
            </dd>
          </>
        )}
      </dl>

      <IfPermitted permissions="clusterconfigentry:edit">
        <Button
          bsStyle="info"
          bsSize="xs"
          onClick={() => {
            setShowModal(true);
          }}>
          Edit configuration
        </Button>
      </IfPermitted>
      <Modal show={showModal} onHide={resetConfig}>
        <Formik onSubmit={handleSubmit} initialValues={curConfig}>
          {({ values, setFieldValue, isSubmitting }) => (
            <Form>
              <Modal.Header>
                <Modal.Title>{modalTitle}</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <Row>
                  <Col sm={6}>
                    <FormikInput id="enabled" type="checkbox" label="Enable Geo-Location processor" name="enabled" />
                  </Col>
                  <Col sm={6}>
                    <FormikInput
                      id="enforce_graylog_schema"
                      type="checkbox"
                      disabled={!values.enabled}
                      label="Enforce default schema"
                      name="enforce_graylog_schema"
                    />
                  </Col>
                </Row>
                <Field id="db_vendor_type_select" name="db_vendor_type_field">
                  {() => (
                    <Input id="db_vendor_type_input" label="Select the GeoIP database vendor">
                      <Select
                        id="db_vendor_type"
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
                        }}
                      />
                    </Input>
                  )}
                </Field>
                <FormikInput
                  id="city_db_path"
                  type="text"
                  disabled={!values.enabled}
                  label="Path to the city database"
                  name="city_db_path"
                  required
                />
                <FormikInput
                  id="asn_db_path"
                  type="text"
                  disabled={!values.enabled}
                  label="Path to the ASN database"
                  name="asn_db_path"
                />
                <TimeUnitInput
                  label="Database refresh interval"
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
                  units={['SECONDS', 'MINUTES', 'HOURS', 'DAYS']}
                />

                <Field id="pull_from_cloud_select" name="pull_from_cloud_field">
                  {() => (
                    <Input
                      id="pull_from_cloud_input"
                      label={
                        <>
                          Pull files from cloud storage bucket <InputOptionalInfo />
                        </>
                      }>
                      <Select
                        id="pull_from_cloud"
                        name="pull_from_cloud"
                        placeholder="Select cloud storage"
                        required
                        disabled={!values.enabled}
                        options={cloudStorageOptions}
                        matchProp="label"
                        value={values.pull_from_cloud}
                        onChange={(option) => {
                          setFieldValue('pull_from_cloud', option);

                          if (option !== CLOUD_STORAGE_OPTION.GCS) {
                            setFieldValue('gcs_project_id', undefined);
                          }
                        }}
                      />
                    </Input>
                  )}
                </Field>

                {values.pull_from_cloud === CLOUD_STORAGE_OPTION.GCS && (
                  <FormikInput
                    id="gcs_project_id"
                    type="text"
                    disabled={!values.enabled}
                    label={
                      <>
                        Googe Cloud Storage Project ID <InputOptionalInfo />
                      </>
                    }
                    name="gcs_project_id"
                  />
                )}
              </Modal.Body>
              <Modal.Footer>
                <ModalSubmit
                  onCancel={resetConfig}
                  isSubmitting={isSubmitting}
                  isAsyncSubmit
                  submitButtonText="Update configuration"
                  submitLoadingText="Updating configuration..."
                />
              </Modal.Footer>
            </Form>
          )}
        </Formik>
      </Modal>
    </div>
  );
};

export default GeoIpResolverConfig;
