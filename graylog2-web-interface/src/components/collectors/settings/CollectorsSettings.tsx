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
import moment from 'moment';
import styled, { css } from 'styled-components';

import { Alert, Row, Col } from 'components/bootstrap';
import { FormikInput, Spinner } from 'components/common';
import TimeUnitInput, { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import FormSubmit from 'components/common/FormSubmit';
import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';

import IngestEndpointStatus from './IngestEndpointStatus';

import { useCollectorsConfig, useCollectorInputIds, useCollectorsMutations } from '../hooks';
import type { CollectorsConfigRequest } from '../types';


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
    color: ${theme.colors.gray[60]};
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
  const { data: config, isLoading: isLoadingConfig } = useCollectorsConfig();
  const { updateConfig } = useCollectorsMutations();
  const currentUser = useCurrentUser();
  const isConfigured = !!config?.signing_cert_id;
  const { data: collectorInputIds = [], isLoading: isLoadingInputIds } = useCollectorInputIds();

  const canCreateInputs = isPermitted(currentUser?.permissions, [
    'inputs:create',
    'input_types:create:org.graylog.collectors.input.CollectorIngestHttpInput',
  ]);

  const showCreateInputCheckbox = !isConfigured && !isLoadingInputIds && collectorInputIds.length === 0 && canCreateInputs;

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
    [updateConfig, showCreateInputCheckbox],
  );

  if (isLoadingConfig) {
    return <Spinner />;
  }

  return (
    <>
      <Row className="content">
        {!isConfigured && (
          <Col md={12}>
            <Alert bsStyle="warning">
              Collectors have not been set up yet. Configure the ingest endpoint and basic settings below to get started.
            </Alert>
          </Col>
        )}
        <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} enableReinitialize>
          {({ isSubmitting, setFieldValue, values, errors }) => (
            <Form>
              <Col md={6}>
                <SectionTitle>Ingest Endpoint</SectionTitle>
                <HelpText>
                  The hostname and port that managed collectors use to send their data.
                  This is typically the address of a load balancer or the server itself.
                </HelpText>

                <FormikInput
                  id="http-hostname"
                  type="text"
                  label="Hostname"
                  name="http_hostname"
                  placeholder="e.g. otlp.example.com"
                />
                <FormikInput
                  id="http-port"
                  type="number"
                  label="Port"
                  name="http_port"
                />

                {showCreateInputCheckbox && (
                  <FormikInput
                    id="create-input"
                    type="checkbox"
                    label="Create ingest input"
                    name="create_input"
                  />
                )}
              </Col>

              <Col md={6}>
                <SectionTitle>Collector Lifecycle</SectionTitle>

                <TimeUnitInput
                  label="Offline threshold"
                  update={(value: number, unit: string) => {
                    setFieldValue('offline_value', value);
                    setFieldValue('offline_unit', unit);
                  }}
                  value={values.offline_value}
                  unit={values.offline_unit}
                  units={THRESHOLD_UNITS}
                  required
                  hideCheckbox
                  help={errors.offline_value
                    ? <span className="text-danger">{errors.offline_value}</span>
                    : "Collectors that haven't reported within this time are shown as offline."}
                />

                <TimeUnitInput
                  label="Default visibility"
                  update={(value: number, unit: string) => {
                    setFieldValue('visibility_value', value);
                    setFieldValue('visibility_unit', unit);
                  }}
                  value={values.visibility_value}
                  unit={values.visibility_unit}
                  units={THRESHOLD_UNITS}
                  required
                  hideCheckbox
                  help={errors.visibility_value
                    ? <span className="text-danger">{errors.visibility_value}</span>
                    : "Collectors that haven't reported within this time are hidden from the default view. Users can adjust or remove this filter in the instances table."}
                />

                <TimeUnitInput
                  label="Expiration threshold"
                  update={(value: number, unit: string) => {
                    setFieldValue('expiration_value', value);
                    setFieldValue('expiration_unit', unit);
                  }}
                  value={values.expiration_value}
                  unit={values.expiration_unit}
                  units={THRESHOLD_UNITS}
                  required
                  hideCheckbox
                  help={errors.expiration_value
                    ? <span className="text-danger">{errors.expiration_value}</span>
                    : "Collectors that haven't reported within this time are permanently removed."}
                />
              </Col>

              <Col md={12}>
                <FormSubmit
                  isAsyncSubmit
                  submitButtonText="Update settings"
                  submitLoadingText="Updating..."
                  isSubmitting={isSubmitting}
                  displayCancel={false}
                />
              </Col>
            </Form>
          )}
        </Formik>
      </Row>

      <IngestEndpointStatus defaultPort={config?.http?.port ?? 14401} isInitialSetup={!isConfigured} />
    </>
  );
};

export default CollectorsSettings;
