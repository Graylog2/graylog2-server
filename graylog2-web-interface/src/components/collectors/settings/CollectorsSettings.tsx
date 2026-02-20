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

import { Button, Input, Alert, Row, Col } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import InputStateBadge from 'components/inputs/InputStateBadge';
import useInputsStates from 'hooks/useInputsStates';

import { useCollectorsConfig, useInput } from '../hooks/useCollectors';
import useCollectorsMutations from '../hooks/useCollectorsMutations';
import type { CollectorsConfigRequest } from '../types';

type EndpointFormState = {
  enabled: boolean;
  hostname: string;
  port: number;
};

const DEFAULT_HTTP: EndpointFormState = { enabled: false, hostname: '', port: 14401 };
const DEFAULT_GRPC: EndpointFormState = { enabled: false, hostname: '', port: 14402 };

const CollectorsSettings = () => {
  const { data: config, isLoading: isLoadingConfig } = useCollectorsConfig();
  const { updateConfig, isUpdatingConfig } = useCollectorsMutations();
  const isConfigured = !!config?.opamp_ca_id;
  const { data: inputStates } = useInputsStates({ enabled: isConfigured });
  const { data: httpInput } = useInput(config?.http?.input_id ?? null);
  const { data: grpcInput } = useInput(config?.grpc?.input_id ?? null);

  const [http, setHttp] = useState<EndpointFormState>(DEFAULT_HTTP);
  const [grpc, setGrpc] = useState<EndpointFormState>(DEFAULT_GRPC);
  const [initialized, setInitialized] = useState(false);

  useEffect(() => {
    if (config && !initialized) {
      setHttp({
        enabled: config.http.enabled,
        hostname: config.http.hostname,
        port: config.http.port,
      });
      setGrpc({
        enabled: config.grpc.enabled,
        hostname: config.grpc.hostname,
        port: config.grpc.port,
      });
      setInitialized(true);
    }
  }, [config, initialized]);

  if (isLoadingConfig) {
    return <Spinner />;
  }

  const handleSave = async () => {
    const request: CollectorsConfigRequest = {
      http: { enabled: http.enabled, hostname: http.hostname, port: http.port },
      grpc: { enabled: grpc.enabled, hostname: grpc.hostname, port: grpc.port },
    };
    await updateConfig(request);
    setInitialized(false);
  };

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
        <Col md={12}>
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

          <h3>gRPC</h3>
          <Input id="grpc-enabled"
                 type="checkbox"
                 label="Enabled"
                 checked={grpc.enabled}
                 onChange={(e) => setGrpc({ ...grpc, enabled: (e.target as HTMLInputElement).checked })} />
          <Input id="grpc-hostname"
                 type="text"
                 label="Hostname"
                 value={grpc.hostname}
                 placeholder="e.g. otlp-grpc.example.com"
                 onChange={(e) => setGrpc({ ...grpc, hostname: (e.target as HTMLInputElement).value })}
                 disabled={!grpc.enabled} />
          <Input id="grpc-port"
                 type="number"
                 label="Port"
                 value={grpc.port}
                 onChange={(e) => setGrpc({ ...grpc, port: Number((e.target as HTMLInputElement).value) })}
                 disabled={!grpc.enabled} />

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
            {grpcInput && (
              <p>
                <strong>gRPC:</strong>{' '}
                <InputStateBadge input={grpcInput} inputStates={inputStates} />{' '}
                <Link to={Routes.SYSTEM.INPUT_DIAGNOSIS(grpcInput.id)}>View Diagnostics</Link>
              </p>
            )}
            {!httpInput && !grpcInput && (
              <p>No ingest endpoints are running.</p>
            )}
          </Col>
        </Row>
      )}
    </>
  );
};

export default CollectorsSettings;
