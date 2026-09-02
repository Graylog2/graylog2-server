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
import { useCallback, useEffect, useRef, useState } from 'react';
import { Formik, Form } from 'formik';

import { Alert, Button, Table } from 'components/bootstrap';
import { FormikInput, Group, Link, Section, Spinner, Stack, Text } from 'components/common';
import InputStateBadge from 'components/inputs/InputStateBadge';
import Routes from 'routing/Routes';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useInputsStates from 'hooks/useInputsStates';
import AppConfig from 'util/AppConfig';

import {
  useCollectorInputIds,
  useCollectorInputDetails,
  useCollectorInputMutations,
  useCollectorPermissions,
  useCollectorsMutations,
} from '../../hooks';
import useSendCollectorsTelemetry from '../../hooks/useSendCollectorsTelemetry';
import { classifyHostname } from '../../hooks/telemetry-helpers';
import type { CollectorsConfig, CollectorsConfigRequest } from '../../types';

type Props = {
  // The current config. Before the first save this is the server-derived default (hostname/port seed the
  // form, thresholds are echoed back unchanged); afterwards it carries `signing_cert_id`.
  config: CollectorsConfig;
  // Called after the endpoint was saved. The wizard mints its enrollment token only then, because the token
  // signing key is created by that first save.
  onConfirmed?: () => void;
};

type FormValues = {
  http_hostname: string;
  http_port: number;
};

const buildRequest = (config: CollectorsConfig, values: FormValues, createInput: boolean): CollectorsConfigRequest => ({
  http: { hostname: values.http_hostname, port: values.http_port },
  collector_offline_threshold: config.collector_offline_threshold,
  collector_default_visibility_threshold: config.collector_default_visibility_threshold,
  collector_expiration_threshold: config.collector_expiration_threshold,
  create_input: createInput,
});

/**
 * The "confirm how Collectors reach this cluster" strip of the onboarding wizard. Before the collectors config
 * exists it asks for the external endpoint; saving it performs the one-time server-side bootstrap. Once the
 * config exists it reports whether an ingest input is actually running, mirroring the facts the Settings page
 * shows, so a user learns up front when Collectors would enroll but never deliver data.
 *
 * Permission gating for the initial setup lives in FirstOnboarding: the wizard is not shown at all to a user who
 * could not create the ingest input. Here only the "move the existing input" edit is permission-checked, with a
 * notice up front and no attempt when the user lacks it.
 */
const IngestEndpointStrip = ({ config, onConfirmed = undefined }: Props) => {
  const { updateConfig } = useCollectorsMutations();
  const { createCollectorInput, updateCollectorInputPort } = useCollectorInputMutations();
  const { canCreateIngestInput, canEditIngestInput } = useCollectorPermissions();
  const { data: collectorInputIds = [], isLoading: isLoadingInputIds } = useCollectorInputIds();
  const { loadedInputs, isLoading: isLoadingInputDetails } = useCollectorInputDetails();
  const { data: inputStates, isLoading: isLoadingInputStates } = useInputsStates({
    enabled: collectorInputIds.length > 0,
  });
  const sendTelemetry = useSendCollectorsTelemetry();
  const isCloud = AppConfig.isCloud();
  const [editing, setEditing] = useState(false);

  const isConfigured = !!config.signing_cert_id;
  const endpoint = `${config.http.hostname}:${config.http.port}`;

  // In Cloud the ingest endpoint is server-provisioned and there is no persisted input.
  const hasInputs = collectorInputIds.length > 0;
  const needsInput = !isCloud && !isLoadingInputIds && !hasInputs;
  // The wizard only runs when the input exists or can be created (see FirstOnboarding), so this is plain fact.
  const willCreateInput = needsInput && canCreateIngestInput;

  // Moving an existing input to a new port is an input edit.
  const canEditInputs = loadedInputs.length > 0 && loadedInputs.every((input) => canEditIngestInput(input.id));
  const cannotMoveExistingInput = hasInputs && !canEditInputs;
  const existingInputPorts = [...new Set(loadedInputs.map((input) => Number(input.attributes?.port)))].filter(
    (port) => !Number.isNaN(port),
  );

  const hasRunningInput = loadedInputs.some((input) => {
    const nodeStates = inputStates?.[input.id];
    if (!nodeStates) return false;

    return Object.values(nodeStates).some((entry) => entry.state === 'RUNNING');
  });

  const submit = useCallback(
    async (values: FormValues) => {
      await updateConfig(buildRequest(config, values, willCreateInput));

      // Keep the input on the port collectors are told to use. Hostname changes never touch the input: its bind
      // address is a separate concern (load balancer in front, etc.).
      const inputsToMove = canEditInputs
        ? loadedInputs.filter((input) => Number(input.attributes?.port) !== values.http_port)
        : [];
      await Promise.all(inputsToMove.map((input) => updateCollectorInputPort({ input, port: values.http_port })));

      sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.SETTINGS.UPDATED, {
        app_action_value: 'onboarding-endpoint-confirm',
        http_hostname_kind: classifyHostname(values.http_hostname ?? ''),
        http_port: values.http_port,
        http_hostname_changed: config.http.hostname !== values.http_hostname,
        http_port_changed: config.http.port !== values.http_port,
        create_input: willCreateInput,
        inputs_moved: inputsToMove.length,
        input_count: collectorInputIds.length,
        initial_setup: !isConfigured,
      });

      setEditing(false);
      onConfirmed?.();
    },
    [
      config,
      willCreateInput,
      canEditInputs,
      loadedInputs,
      updateConfig,
      updateCollectorInputPort,
      sendTelemetry,
      collectorInputIds.length,
      isConfigured,
      onConfirmed,
    ],
  );

  // Cloud: nothing to ask, bootstrap once on mount. The ref guards against a second run when the mutation's
  // invalidation re-renders this component before the refetched config arrives.
  const autoSubmitted = useRef(false);
  useEffect(() => {
    if (!isCloud || isConfigured || autoSubmitted.current) return;

    autoSubmitted.current = true;
    submit({ http_hostname: config.http.hostname, http_port: config.http.port }).catch(() => {
      // Error notification handled by useCollectorsMutations onError callback
    });
  }, [isCloud, isConfigured, config, submit]);

  if (isCloud && !isConfigured) {
    return <Spinner text="Setting up Collectors..." />;
  }

  if (!isConfigured || editing) {
    return (
      <Formik<FormValues>
        initialValues={{ http_hostname: config.http.hostname, http_port: config.http.port }}
        onSubmit={(values) =>
          submit(values).catch(() => {
            // Error notification handled by useCollectorsMutations onError callback
          })
        }>
        {({ isSubmitting }) => (
          <Form>
            <Section
              title="Confirm how Collectors reach this cluster"
              titleAs="h4"
              actions={
                <Group gap="xs" wrap="nowrap">
                  <div style={{ width: 260 }}>
                    <FormikInput
                      id="ingest-hostname"
                      type="text"
                      name="http_hostname"
                      label="External hostname"
                      labelClassName="sr-only"
                      formGroupClassName="no-bm"
                      placeholder="e.g. otlp.example.com"
                      disabled={isSubmitting}
                    />
                  </div>
                  <Text span c="dimmed" fw={600} aria-hidden="true">
                    :
                  </Text>
                  <div style={{ width: 100 }}>
                    <FormikInput
                      id="ingest-port"
                      type="number"
                      name="http_port"
                      label="External port"
                      labelClassName="sr-only"
                      formGroupClassName="no-bm"
                      disabled={isSubmitting}
                    />
                  </div>
                  <Button type="submit" bsStyle="primary" disabled={isSubmitting}>
                    {isSubmitting ? 'Saving...' : 'Confirm'}
                  </Button>
                  {editing && (
                    <Button bsStyle="link" onClick={() => setEditing(false)} disabled={isSubmitting}>
                      Cancel
                    </Button>
                  )}
                </Group>
              }>
              <Stack gap="xs">
                <Text size="sm" c="dimmed">
                  Collectors send their logs to this address. It must be reachable from every Collector host: a load
                  balancer, or this server.
                </Text>
                {cannotMoveExistingInput && (
                  <Text size="sm">
                    {`The existing ingest input ${loadedInputs.length === 1 ? 'stays' : 'inputs stay'} on port ${existingInputPorts.join(', ')}: moving it needs input edit permissions you do not have. If you change the port here, ask an administrator to move the input as well.`}
                  </Text>
                )}
              </Stack>
            </Section>
          </Form>
        )}
      </Formik>
    );
  }

  const changeButton = (
    <Button bsStyle="link" onClick={() => setEditing(true)}>
      Change
    </Button>
  );

  const endpointText = (
    <Text span ff="monospace" fw={700}>
      {endpoint}
    </Text>
  );

  // Headline row of a status alert: message left, "Change" right.
  const headline = (message: React.ReactNode) => (
    <Group justify="space-between" wrap="wrap">
      <span>{message}</span>
      {changeButton}
    </Group>
  );

  if (isCloud) {
    return <Alert bsStyle="success">Collectors reach this cluster at {endpointText}</Alert>;
  }

  if (isLoadingInputIds || isLoadingInputDetails || (hasInputs && isLoadingInputStates)) {
    return <Spinner />;
  }

  if (!hasInputs) {
    return (
      <Alert bsStyle="warning" title={headline(<>No ingest input exists for {endpointText}</>)}>
        <Stack gap="xs" align="flex-start">
          <Text>Collectors can enroll, but they will not be able to send data until an input is created.</Text>
          {canCreateIngestInput ? (
            <Button bsSize="small" onClick={() => createCollectorInput()}>
              Create input
            </Button>
          ) : (
            <Text>{`Ask an administrator to create a Collector Ingest (HTTP) input on port ${config.http.port}.`}</Text>
          )}
        </Stack>
      </Alert>
    );
  }

  if (!hasRunningInput) {
    return (
      <Alert bsStyle="warning" title={headline(<>The ingest input on {endpointText} is not running</>)}>
        <Stack gap="xs">
          <Text>Collectors can enroll, but they will not be able to send data until an input is running.</Text>
          <Table condensed>
            <tbody>
              {loadedInputs.map((input) => (
                <tr key={input.id}>
                  <td>{input.title}</td>
                  <td>
                    <InputStateBadge input={input} inputStates={inputStates} />
                  </td>
                  <td>
                    {String(input.attributes?.bind_address ?? '')}:{String(input.attributes?.port ?? '')}
                  </td>
                  <td>
                    <Link to={`${Routes.SYSTEM.INPUTS}?query=id%3A${input.id}`}>Manage input</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </Stack>
      </Alert>
    );
  }

  return <Alert bsStyle="success">{headline(<>Collectors reach this cluster at {endpointText}</>)}</Alert>;
};

export default IngestEndpointStrip;
