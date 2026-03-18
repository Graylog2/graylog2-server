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
import { useState, useEffect } from 'react';
import moment from 'moment';

import { Button, Input, Alert, Row, Col } from 'components/bootstrap';
import { Link, Spinner } from 'components/common';
import TimeUnitInput, { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import Routes from 'routing/Routes';
import InputStateBadge from 'components/inputs/InputStateBadge';
import useInput from 'hooks/useInput';
import useInputsStates from 'hooks/useInputsStates';

import { useCollectorsConfig } from '../hooks';
import useCollectorsMutations from '../hooks/useCollectorsMutations';
import type { CollectorsConfigRequest } from '../types';

type EndpointFormState = {
  enabled: boolean;
  hostname: string;
  port: number;
};

const DEFAULT_HTTP: EndpointFormState = { enabled: false, hostname: '', port: 14401 };

const THRESHOLD_UNITS = ['DAYS', 'HOURS', 'MINUTES'];

const CollectorsSettings = () => {
  const { data: config, isLoading: isLoadingConfig } = useCollectorsConfig();
  const { updateConfig, isUpdatingConfig } = useCollectorsMutations();
  const isConfigured = !!config?.signing_cert_id;
  const { data: inputStates } = useInputsStates({ enabled: isConfigured });
  const { data: httpInput } = useInput(config?.http?.input_id);

  const [http, setHttp] = useState<EndpointFormState>(DEFAULT_HTTP);
  const [initialized, setInitialized] = useState(false);
  const [offlineValue, setOfflineValue] = useState<number>(5);
  const [offlineUnit, setOfflineUnit] = useState<string>('MINUTES');
  const [visibilityValue, setVisibilityValue] = useState<number>(1);
  const [visibilityUnit, setVisibilityUnit] = useState<string>('DAYS');
  const [expirationValue, setExpirationValue] = useState<number>(7);
  const [expirationUnit, setExpirationUnit] = useState<string>('DAYS');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (config && !initialized) {
      setHttp({
        enabled: config.http.enabled,
        hostname: config.http.hostname,
        port: config.http.port,
      });

      const offline = extractDurationAndUnit(config.collector_offline_threshold, THRESHOLD_UNITS);
      setOfflineValue(offline.duration);
      setOfflineUnit(offline.unit);

      const visibility = extractDurationAndUnit(config.collector_default_visibility_threshold, THRESHOLD_UNITS);
      setVisibilityValue(visibility.duration);
      setVisibilityUnit(visibility.unit);

      const expiration = extractDurationAndUnit(config.collector_expiration_threshold, THRESHOLD_UNITS);
      setExpirationValue(expiration.duration);
      setExpirationUnit(expiration.unit);

      setInitialized(true);
    }
  }, [config, initialized]);

  if (isLoadingConfig) {
    return <Spinner />;
  }

  const handleOfflineChange = (value: number, unit: string, _checked: boolean) => {
    setOfflineValue(value);
    setOfflineUnit(unit);
    setFieldErrors({});
  };

  const handleVisibilityChange = (value: number, unit: string, _checked: boolean) => {
    setVisibilityValue(value);
    setVisibilityUnit(unit);
    setFieldErrors({});
  };

  const handleExpirationChange = (value: number, unit: string, _checked: boolean) => {
    setExpirationValue(value);
    setExpirationUnit(unit);
    setFieldErrors({});
  };

  const handleSave = async () => {
    const request: CollectorsConfigRequest = {
      http: { enabled: http.enabled, hostname: http.hostname, port: http.port },
      collector_offline_threshold: moment.duration(offlineValue, offlineUnit as moment.unitOfTime.DurationConstructor).toISOString(),
      collector_default_visibility_threshold: moment.duration(visibilityValue, visibilityUnit as moment.unitOfTime.DurationConstructor).toISOString(),
      collector_expiration_threshold: moment.duration(expirationValue, expirationUnit as moment.unitOfTime.DurationConstructor).toISOString(),
    };

    setFieldErrors({});

    try {
      await updateConfig(request);
      setInitialized(false);
    } catch (error: unknown) {
      const validationErrors = (error as { additional?: { body?: { validation_errors?: Record<string, Array<{ error: string }>> } } })
        ?.additional?.body?.validation_errors;

      if (validationErrors) {
        const extracted: Record<string, string> = {};

        Object.entries(validationErrors).forEach(([field, errors]) => {
          if (errors?.[0]?.error) {
            extracted[field] = errors[0].error;
          }
        });

        setFieldErrors(extracted);
      }
    }
  };

  const offlineError = fieldErrors['collector_offline_threshold'];
  const visibilityError = fieldErrors['collector_default_visibility_threshold'];
  const expirationError = fieldErrors['collector_expiration_threshold'];

  return (
    <>
      {!isConfigured && (
        <Row className="content">
          <Col md={12}>
            <Alert bsStyle="warning">
              Collectors have not been set up yet. Configure the ingest endpoints below and save to get started.
            </Alert>
          </Col>
        </Row>
      )}

      <Row className="content">
        <Col md={6}>
          <h2>Ingest Endpoints</h2>

          <h3>HTTP</h3>
          <Input id="http-enabled"
                 type="checkbox"
                 label="Enabled"
                 checked={http.enabled}
                 onChange={(e) => setHttp({ ...http, enabled: (e.target as HTMLInputElement).checked })} />
          <Input id="http-hostname"
                 type="text"
                 label="Hostname"
                 value={http.hostname}
                 placeholder="e.g. otlp.example.com"
                 onChange={(e) => setHttp({ ...http, hostname: (e.target as HTMLInputElement).value })}
                 disabled={!http.enabled} />
          <Input id="http-port"
                 type="number"
                 label="Port"
                 value={http.port}
                 onChange={(e) => setHttp({ ...http, port: Number((e.target as HTMLInputElement).value) })}
                 disabled={!http.enabled} />

          <h2>Collector Lifecycle</h2>

          <TimeUnitInput
            label="Offline threshold"
            update={handleOfflineChange}
            value={offlineValue}
            unit={offlineUnit}
            units={THRESHOLD_UNITS}
            required
            hideCheckbox
            help={offlineError
              ? <span className="text-danger">{offlineError}</span>
              : 'Collectors that haven\'t reported within this time are shown as offline.'} />

          <TimeUnitInput
            label="Default visibility"
            update={handleVisibilityChange}
            value={visibilityValue}
            unit={visibilityUnit}
            units={THRESHOLD_UNITS}
            required
            hideCheckbox
            help={visibilityError
              ? <span className="text-danger">{visibilityError}</span>
              : 'Collectors that haven\'t reported within this time are hidden from the default view. Users can adjust or remove this filter in the instances table.'} />

          <TimeUnitInput
            label="Expiration threshold"
            update={handleExpirationChange}
            value={expirationValue}
            unit={expirationUnit}
            units={THRESHOLD_UNITS}
            required
            hideCheckbox
            help={expirationError
              ? <span className="text-danger">{expirationError}</span>
              : 'Collectors that haven\'t reported within this time are permanently removed.'} />

          <Button bsStyle="primary"
                  onClick={handleSave}
                  disabled={isUpdatingConfig}>
            {isUpdatingConfig ? 'Saving...' : 'Save'}
          </Button>
        </Col>
      </Row>

      {isConfigured && (
        <Row className="content">
          <Col md={12}>
            <h2>Ingest Endpoint Status</h2>
            {httpInput && (
              <p>
                <strong>HTTP:</strong>{' '}
                <InputStateBadge input={httpInput} inputStates={inputStates} />{' '}
                <Link to={Routes.SYSTEM.INPUT_DIAGNOSIS(httpInput.id)}>View Diagnostics</Link>
              </p>
            )}
            {!httpInput && (
              <p>No ingest endpoints are running.</p>
            )}
          </Col>
        </Row>
      )}
    </>
  );
};

export default CollectorsSettings;
