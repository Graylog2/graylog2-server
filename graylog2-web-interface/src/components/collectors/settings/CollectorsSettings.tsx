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

import { Alert, Row, Col } from 'components/bootstrap';
import { FormikInput, Link, Spinner } from 'components/common';
import TimeUnitInput, { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import Routes from 'routing/Routes';
import InputStateBadge from 'components/inputs/InputStateBadge';
import useInput from 'hooks/useInput';
import useInputsStates from 'hooks/useInputsStates';
import FormSubmit from 'components/common/FormSubmit';

import { useCollectorsConfig, useCollectorsMutations } from '../hooks';
import type { CollectorsConfigRequest } from '../types';

type FormValues = {
  http_enabled: boolean;
  http_hostname: string;
  http_port: number;
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
  const isConfigured = !!config?.signing_cert_id;
  const { data: inputStates } = useInputsStates({ enabled: isConfigured });
  const { data: httpInput } = useInput(config?.http?.input_id);

  const initialValues: FormValues = useMemo(() => {
    if (!config) {
      return {
        http_enabled: false,
        http_hostname: '',
        http_port: 14401,
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
      http_enabled: config.http.enabled,
      http_hostname: config.http.hostname,
      http_port: config.http.port,
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
        http: { enabled: values.http_enabled, hostname: values.http_hostname, port: values.http_port },
        collector_offline_threshold: moment
          .duration(values.offline_value, values.offline_unit as moment.unitOfTime.DurationConstructor)
          .toISOString(),
        collector_default_visibility_threshold: moment
          .duration(values.visibility_value, values.visibility_unit as moment.unitOfTime.DurationConstructor)
          .toISOString(),
        collector_expiration_threshold: moment
          .duration(values.expiration_value, values.expiration_unit as moment.unitOfTime.DurationConstructor)
          .toISOString(),
      };

      try {
        await updateConfig(request);
      } catch (error: unknown) {
        const validationErrors = (
          error as { additional?: { body?: { validation_errors?: Record<string, Array<{ error: string }>> } } }
        )?.additional?.body?.validation_errors;

        if (validationErrors) {
          const extracted: Record<string, string> = {};

          Object.entries(validationErrors).forEach(([field, errors]) => {
            if (errors?.[0]?.error) {
              extracted[field] = errors[0].error;
            }
          });

          setErrors(extracted);
        }
      }
    },
    [updateConfig],
  );

  if (isLoadingConfig) {
    return <Spinner />;
  }

  return (
    <>
      <Row className="content">
        <Col md={6}>
          {!isConfigured && (
            <Alert bsStyle="warning">
              Collectors have not been set up yet. Configure the ingest endpoints below and save to get started.
            </Alert>
          )}
          <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} enableReinitialize>
            {({ isSubmitting, setFieldValue, values }) => (
              <Form>
                <h2>Ingest Endpoints</h2>

                <h3>HTTP</h3>
                <FormikInput id="http-enabled" type="checkbox" label="Enabled" name="http_enabled" />
                <FormikInput
                  id="http-hostname"
                  type="text"
                  label="Hostname"
                  name="http_hostname"
                  placeholder="e.g. otlp.example.com"
                  disabled={!values.http_enabled}
                />
                <FormikInput
                  id="http-port"
                  type="number"
                  label="Port"
                  name="http_port"
                  disabled={!values.http_enabled}
                />

                <h2>Collector Lifecycle</h2>

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
                  help="Collectors that haven't reported within this time are shown as offline."
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
                  help="Collectors that haven't reported within this time are hidden from the default view. Users can adjust or remove this filter in the instances table."
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
                  help="Collectors that haven't reported within this time are permanently removed."
                />
                <FormSubmit
                  isAsyncSubmit
                  submitButtonText="Update settings"
                  submitLoadingText="Updating..."
                  isSubmitting={isSubmitting}
                  displayCancel={false}
                />
              </Form>
            )}
          </Formik>
        </Col>
      </Row>

      {isConfigured && (
        <Row className="content">
          <Col md={12}>
            <h2>Ingest Endpoint Status</h2>
            {httpInput && (
              <p>
                <strong>HTTP:</strong> <InputStateBadge input={httpInput} inputStates={inputStates} />{' '}
                <Link to={Routes.SYSTEM.INPUT_DIAGNOSIS(httpInput.id)}>View Diagnostics</Link>
              </p>
            )}
            {!httpInput && <p>No ingest endpoints are running.</p>}
          </Col>
        </Row>
      )}
    </>
  );
};

export default CollectorsSettings;
