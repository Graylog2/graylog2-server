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
import { Field, Form, Formik } from 'formik';
import { Trans, useTranslation } from 'react-i18next';

import { IfPermitted, Select, TimeUnitInput, ModalSubmit, InputOptionalInfo } from 'components/common';
import { Button, Col, Input, Modal, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import { DocumentationLink } from 'components/support';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import type { GeoVendorType, GeoIpConfigType, OptionType } from 'components/maps/configurations/types';
import { CLOUD_STORAGE_OPTION } from 'components/maps/configurations/types';
import GCSGeoIpFormGroup from 'components/maps/configurations/GCSGeoIpFormGroup';
import ABSGeoIpFormGroup from 'components/maps/configurations/ABSGeoIpFormGroup';

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
  azure_endpoint: undefined,
  azure_account_key: undefined,
  azure_account: undefined,
  azure_container: undefined,
};

const GeoIpResolverConfig = ({ config = defaultConfig, updateConfig }: Props) => {
  const { t } = useTranslation('common');
  const [showModal, setShowModal] = useState(false);

  const sendTelemetry = useSendTelemetry('geolocation-processor');

  const resetConfig = () => {
    setShowModal(false);
  };

  const handleSubmit = (values: GeoIpConfigType) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.GEOLOCATION_CONFIGURATION_UPDATED, {
      app_pathname: 'configurations',
    });

    const normalizedValues = {
      ...values,
      azure_endpoint:
        values.azure_endpoint && values.azure_endpoint.trim() !== '' ? values.azure_endpoint.trim() : null,
      azure_account_key:
        values.azure_account_key && 'is_set' in values.azure_account_key
          ? { keep_value: true }
          : values.azure_account_key,
    };

    return updateConfig(normalizedValues).then((value: GeoIpConfigType) => {
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
    { value: CLOUD_STORAGE_OPTION.ABS, label: 'Azure Blob Storage' },
  ];

  const activeVendorType = (type: GeoVendorType) => availableVendorTypes().filter((t) => t.value === type)[0].label;

  const modalTitle = t('geoip_resolver.modal_title');

  return (
    <div>
      <h3>{t('geoip_resolver.title')}</h3>

      <p>
        <Trans
          t={t}
          i18nKey="geoip_resolver.description"
          components={{
            bold: <strong />,
            docsLink: <DocumentationLink page="geolocation" text={t('links.documentation')} />,
          }}
        />
      </p>

      <dl className="deflist">
        <dt>{t('geoip_resolver.enabled_summary')}</dt>
        <dd>{config.enabled === true ? t('status_icon.yes') : t('status_icon.no')}</dd>
        {config.enabled && (
          <>
            <dt>{t('geoip_resolver.enforce_schema_summary')}</dt>
            <dd>{config.enforce_graylog_schema === true ? t('status_icon.yes') : t('status_icon.no')}</dd>
            <dt>{t('geoip_resolver.vendor_type_summary')}</dt>
            <dd>{activeVendorType(config.db_vendor_type)}</dd>
            <dt>{t('geoip_resolver.city_db_path_summary')}</dt>
            <dd>{config.city_db_path}</dd>
            <dt>{t('geoip_resolver.asn_db_path_summary')}</dt>
            <dd>{config.asn_db_path === '' ? '-' : config.asn_db_path}</dd>
            <dt>{t('geoip_resolver.refresh_interval_summary')}</dt>
            <dd>
              {config.refresh_interval} {config.refresh_interval_unit}
            </dd>
            <dt>{t('geoip_resolver.pull_from_cloud_summary')}</dt>
            <dd>
              {config.pull_from_cloud
                ? cloudStorageOptions.find((option) => option.value === config.pull_from_cloud)?.label
                : t('status_icon.no')}
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
          {t('geoip_resolver.edit')}
        </Button>
      </IfPermitted>
      <Modal show={showModal} onHide={resetConfig}>
        <Formik onSubmit={handleSubmit} initialValues={config}>
          {({ values, setFieldValue, isSubmitting }) => (
            <Form>
              <Modal.Header>
                <Modal.Title>{modalTitle}</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <Row>
                  <Col sm={6}>
                    <FormikInput
                      id="enabled"
                      type="checkbox"
                      label={t('geoip_resolver.enable_processor')}
                      name="enabled"
                    />
                  </Col>
                  <Col sm={6}>
                    <FormikInput
                      id="enforce_graylog_schema"
                      type="checkbox"
                      disabled={!values.enabled}
                      label={t('geoip_resolver.enforce_schema')}
                      name="enforce_graylog_schema"
                    />
                  </Col>
                </Row>
                <Field id="db_vendor_type_select" name="db_vendor_type_field">
                  {() => (
                    <Input id="db_vendor_type_input" label={t('geoip_resolver.vendor_select')}>
                      <Select
                        id="db_vendor_type"
                        name="db_vendor_type"
                        clearable={false}
                        placeholder={t('geoip_resolver.vendor_select')}
                        required
                        disabled={!values.enabled}
                        options={availableVendorTypes()}
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
                  label={t('geoip_resolver.city_db_path')}
                  name="city_db_path"
                  required
                />
                <FormikInput
                  id="asn_db_path"
                  type="text"
                  disabled={!values.enabled}
                  label={t('geoip_resolver.asn_db_path')}
                  name="asn_db_path"
                />
                <TimeUnitInput
                  label={t('geoip_resolver.refresh_interval')}
                  update={(value, unit) => {
                    setFieldValue('refresh_interval', value);
                    setFieldValue('refresh_interval_unit', unit);
                  }}
                  help={t('geoip_resolver.refresh_interval_help')}
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
                          {t('geoip_resolver.pull_from_cloud')} <InputOptionalInfo />
                        </>
                      }>
                      <Select
                        id="pull_from_cloud"
                        name="pull_from_cloud"
                        placeholder={t('geoip_resolver.select_cloud_storage')}
                        disabled={!values.enabled}
                        options={cloudStorageOptions}
                        value={values.pull_from_cloud}
                        onChange={(option) => {
                          setFieldValue('pull_from_cloud', option);

                          setFieldValue('gcs_project_id', undefined);
                          setFieldValue('azure_endpoint', undefined);
                          setFieldValue('azure_account_key', undefined);
                          setFieldValue('azure_account', undefined);
                          setFieldValue('azure_container', undefined);
                        }}
                      />
                    </Input>
                  )}
                </Field>

                {values.pull_from_cloud === CLOUD_STORAGE_OPTION.GCS && <GCSGeoIpFormGroup />}
                {values.pull_from_cloud === CLOUD_STORAGE_OPTION.ABS && <ABSGeoIpFormGroup />}
              </Modal.Body>
              <Modal.Footer>
                <ModalSubmit
                  onCancel={resetConfig}
                  isSubmitting={isSubmitting}
                  isAsyncSubmit
                  submitButtonText={t('geoip_resolver.update_configuration')}
                  submitLoadingText={t('geoip_resolver.updating_configuration')}
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
