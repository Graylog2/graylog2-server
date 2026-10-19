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
import { useCallback, useMemo } from 'react';
import { Formik, Form } from 'formik';
import { useTranslation } from 'react-i18next';
import moment from 'moment';
import styled, { css } from 'styled-components';

import { Alert, Row, Col } from 'components/bootstrap';
import { FormikInput, Spinner } from 'components/common';
import TimeUnitInput, { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import FormSubmit from 'components/common/FormSubmit';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import usePermissions from 'hooks/usePermissions';
import useInputsStates from 'hooks/useInputsStates';
import AppConfig from 'util/AppConfig';

import IngestEndpointStatus from './IngestEndpointStatus';
import PortMismatchAlert from './PortMismatchAlert';

import {
  useCollectorsConfig,
  useCollectorInputIds,
  useCollectorsMutations,
  useCollectorInputDetails,
  useCollectorPermissions,
} from '../hooks';
import type { CollectorsConfigRequest } from '../types';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import { classifyHostname, classifyInputBind } from '../hooks/telemetry-helpers';

const SectionTitle = styled.h3(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
    border-bottom: 1px solid ${theme.colors.gray[80]};
    padding-bottom: ${theme.spacings.xs};
  `,
);

const HelpText = styled.p(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.text.secondary};
    margin-bottom: ${theme.spacings.md};
  `,
);

type FormValues = {
  http_hostname: string;
  http_port: number;
  create_input: boolean;
  offline_value: number;
  offline_unit: string;
  visibility_value: number;
  visibility_unit: string;
  expiration_value: number;
  expiration_unit: string;
};

const THRESHOLD_UNITS = ['DAYS', 'HOURS', 'MINUTES'];

const CollectorsSettings = () => {
  const { t } = useTranslation('collectors');
  const { data: config, isLoading: isLoadingConfig } = useCollectorsConfig();
  const { updateConfig } = useCollectorsMutations();
  const { isPermitted } = usePermissions();
  const isConfigured = !!config?.signing_cert_id;
  const { data: collectorInputIds = [], isLoading: isLoadingInputIds } = useCollectorInputIds();
  const { loadedInputs: collectorInputs, isLoading: isLoadingInputDetails } = useCollectorInputDetails();
  const { data: inputStates } = useInputsStates({ enabled: collectorInputIds.length > 0 });
  const { canEditConfig } = useCollectorPermissions();

  const isCloud = AppConfig.isCloud();

  const canCreateInputs = isPermitted([
    'inputs:create',
    'input_types:create:org.graylog.collectors.input.CollectorIngestHttpInput',
  ]);

  // In Cloud the ingest endpoint is server-provisioned and there is no persisted input, so input creation is hidden.
  const showCreateInputCheckbox =
    !isCloud && !isConfigured && !isLoadingInputIds && collectorInputIds.length === 0 && canCreateInputs;
  const sendTelemetry = useSendCollectorsTelemetry();

  const initialValues: FormValues = useMemo(() => {
    if (!config) {
      return {
        http_hostname: '',
        http_port: 14401,
        create_input: true,
        offline_value: 5,
        offline_unit: 'MINUTES',
        visibility_value: 1,
        visibility_unit: 'DAYS',
        expiration_value: 7,
        expiration_unit: 'DAYS',
      };
    }

    const offline = extractDurationAndUnit(config.collector_offline_threshold, THRESHOLD_UNITS);
    const visibility = extractDurationAndUnit(config.collector_default_visibility_threshold, THRESHOLD_UNITS);
    const expiration = extractDurationAndUnit(config.collector_expiration_threshold, THRESHOLD_UNITS);

    return {
      http_hostname: config.http.hostname,
      http_port: config.http.port,
      create_input: true,
      offline_value: offline.duration,
      offline_unit: offline.unit,
      visibility_value: visibility.duration,
      visibility_unit: visibility.unit,
      expiration_value: expiration.duration,
      expiration_unit: expiration.unit,
    };
  }, [config]);

  const portMatchesAnyInput = useMemo(() => {
    if (collectorInputs.length === 0) return null;

    return collectorInputs.some((input) => input?.attributes?.port === config?.http?.port);
  }, [collectorInputs, config?.http?.port]);

  const inputBindTypes = useMemo((): 'all_wildcard' | 'all_specific' | 'mixed' | 'none' => {
    if (collectorInputs.length === 0) return 'none';
    const kinds = collectorInputs.map((input) => classifyInputBind(String(input?.attributes?.bind_address ?? '')));

    if (kinds.every((k) => k === 'wildcard')) return 'all_wildcard';
    if (kinds.every((k) => k === 'specific')) return 'all_specific';

    return 'mixed';
  }, [collectorInputs]);

  const hasRunningInput = useMemo(() => {
    if (!inputStates || collectorInputs.length === 0) return false;

    return collectorInputs.some((input) => {
      const nodeStates = inputStates?.[input.id];
      if (!nodeStates) return false;

      return Object.values(nodeStates).some((entry) => entry.state === 'RUNNING');
    });
  }, [inputStates, collectorInputs]);

  const handleSubmit = useCallback(
    async (values: FormValues, { setErrors }: { setErrors: (errors: Record<string, string>) => void }) => {
      const request: CollectorsConfigRequest = {
        http: { hostname: values.http_hostname, port: values.http_port },
        collector_offline_threshold: moment
          .duration(values.offline_value, values.offline_unit as moment.unitOfTime.DurationConstructor)
          .toISOString(),
        collector_default_visibility_threshold: moment
          .duration(values.visibility_value, values.visibility_unit as moment.unitOfTime.DurationConstructor)
          .toISOString(),
        collector_expiration_threshold: moment
          .duration(values.expiration_value, values.expiration_unit as moment.unitOfTime.DurationConstructor)
          .toISOString(),
        create_input: showCreateInputCheckbox && values.create_input,
      };

      try {
        await updateConfig(request);

        const offlineSec = Math.round(
          moment
            .duration(values.offline_value, values.offline_unit as moment.unitOfTime.DurationConstructor)
            .asSeconds(),
        );
        const visibilitySec = Math.round(
          moment
            .duration(values.visibility_value, values.visibility_unit as moment.unitOfTime.DurationConstructor)
            .asSeconds(),
        );
        const expirationSec = Math.round(
          moment
            .duration(values.expiration_value, values.expiration_unit as moment.unitOfTime.DurationConstructor)
            .asSeconds(),
        );

        sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.SETTINGS.UPDATED, {
          app_action_value: 'settings-save',
          http_hostname_kind: classifyHostname(values.http_hostname ?? ''),
          http_port: values.http_port,
          offline_threshold_seconds: offlineSec,
          visibility_threshold_seconds: visibilitySec,
          expiration_threshold_seconds: expirationSec,
          http_hostname_changed: (config?.http?.hostname ?? '') !== values.http_hostname,
          http_port_changed: (config?.http?.port ?? null) !== values.http_port,
          offline_threshold_changed: config?.collector_offline_threshold !== request.collector_offline_threshold,
          visibility_threshold_changed:
            config?.collector_default_visibility_threshold !== request.collector_default_visibility_threshold,
          expiration_threshold_changed:
            config?.collector_expiration_threshold !== request.collector_expiration_threshold,
          port_matches_any_input: portMatchesAnyInput,
          input_bind_types: inputBindTypes,
          has_running_input: hasRunningInput,
          input_count: collectorInputIds.length,
        });
      } catch (error: unknown) {
        const validationErrors = (
          error as { additional?: { body?: { validation_errors?: Record<string, Array<{ error: string }>> } } }
        )?.additional?.body?.validation_errors;

        if (validationErrors) {
          const fieldMapping: Record<string, string> = {
            collector_offline_threshold: 'offline_value',
            collector_default_visibility_threshold: 'visibility_value',
            collector_expiration_threshold: 'expiration_value',
          };
          const mapped: Record<string, string> = {};

          Object.entries(validationErrors).forEach(([field, errors]) => {
            if (errors?.[0]?.error) {
              mapped[fieldMapping[field] ?? field] = errors[0].error;
            }
          });

          setErrors(mapped);
        }
      }
    },
    [
      updateConfig,
      showCreateInputCheckbox,
      config,
      sendTelemetry,
      portMatchesAnyInput,
      inputBindTypes,
      hasRunningInput,
      collectorInputIds.length,
    ],
  );

  if (isLoadingConfig) {
    return <Spinner />;
  }

  return (
    <>
      <Row className="content">
        {!isConfigured && (
          <Col md={12}>
            <Alert bsStyle="info">
              <strong>{t('settings.getting_started')}</strong>
              <p>{isCloud ? t('settings.getting_started_cloud') : t('settings.getting_started_self_managed')}</p>
            </Alert>
          </Col>
        )}
        <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} enableReinitialize>
          {({ isSubmitting, setFieldValue, values, errors }) => (
            <Form>
              <Col md={6}>
                <SectionTitle>{t('settings.ingest_endpoint')}</SectionTitle>
                <HelpText>
                  {isCloud ? t('settings.ingest_endpoint_cloud') : t('settings.ingest_endpoint_help')}
                </HelpText>

                <FormikInput
                  id="http-hostname"
                  type="text"
                  label={t('ingest_endpoint.hostname')}
                  name="http_hostname"
                  disabled={isCloud}
                  placeholder={t('ingest_endpoint.hostname_placeholder')}
                  help={isCloud ? undefined : t('settings.hostname_help')}
                />
                <FormikInput
                  id="http-port"
                  type="number"
                  label={t('ingest_endpoint.port')}
                  name="http_port"
                  disabled={isCloud}
                />

                {!isCloud && (
                  <>
                    <PortMismatchAlert
                      formPort={values.http_port}
                      collectorInputs={collectorInputs}
                      isLoading={isLoadingInputDetails}
                    />

                    {showCreateInputCheckbox && (
                      <FormikInput
                        id="create-input"
                        type="checkbox"
                        label={t('settings.create_input')}
                        name="create_input"
                      />
                    )}
                  </>
                )}
              </Col>

              <Col md={6}>
                <SectionTitle>{t('settings.lifecycle')}</SectionTitle>

                <TimeUnitInput
                  label={t('settings.offline_threshold')}
                  update={(value: number, unit: string) => {
                    setFieldValue('offline_value', value);
                    setFieldValue('offline_unit', unit);
                  }}
                  value={values.offline_value}
                  unit={values.offline_unit}
                  units={THRESHOLD_UNITS}
                  required
                  hideCheckbox
                  help={
                    errors.offline_value ? (
                      <span className="text-danger">{errors.offline_value}</span>
                    ) : (
                      t('settings.offline_threshold_help')
                    )
                  }
                />

                <TimeUnitInput
                  label={t('settings.default_visibility')}
                  update={(value: number, unit: string) => {
                    setFieldValue('visibility_value', value);
                    setFieldValue('visibility_unit', unit);
                  }}
                  value={values.visibility_value}
                  unit={values.visibility_unit}
                  units={THRESHOLD_UNITS}
                  required
                  hideCheckbox
                  help={
                    errors.visibility_value ? (
                      <span className="text-danger">{errors.visibility_value}</span>
                    ) : (
                      t('settings.default_visibility_help')
                    )
                  }
                />

                <TimeUnitInput
                  label={t('settings.expiration_threshold')}
                  update={(value: number, unit: string) => {
                    setFieldValue('expiration_value', value);
                    setFieldValue('expiration_unit', unit);
                  }}
                  value={values.expiration_value}
                  unit={values.expiration_unit}
                  units={THRESHOLD_UNITS}
                  required
                  hideCheckbox
                  help={
                    errors.expiration_value ? (
                      <span className="text-danger">{errors.expiration_value}</span>
                    ) : (
                      t('settings.expiration_threshold_help')
                    )
                  }
                />
              </Col>

              <Col md={12}>
                {canEditConfig && (
                  <FormSubmit
                    isAsyncSubmit
                    submitButtonText={t('settings.confirm')}
                    submitLoadingText={t('fleet_settings.saving')}
                    isSubmitting={isSubmitting}
                    displayCancel={false}
                  />
                )}
              </Col>
            </Form>
          )}
        </Formik>
      </Row>

      {!isCloud && <IngestEndpointStatus isInitialSetup={!isConfigured} />}
    </>
  );
};

export default CollectorsSettings;
