import * as React from 'react';
import { useState, useEffect } from 'react';

import styled, { css } from 'styled-components';
import { Button, Input, Alert, Badge } from 'components/bootstrap';
import { Spinner } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import useInputsStates from 'hooks/useInputsStates';
import type { InputStates } from 'hooks/useInputsStates';

import { useCollectorsConfig } from '../hooks/useCollectors';
import useCollectorsMutations from '../hooks/useCollectorsMutations';
import type { CollectorsConfigRequest } from '../types';

const Section = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.md};
  `,
);

const EndpointStatus = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
`;

type EndpointFormState = {
  enabled: boolean;
  hostname: string;
  port: number;
};

const DEFAULT_HTTP: EndpointFormState = { enabled: true, hostname: '', port: 14401 };
const DEFAULT_GRPC: EndpointFormState = { enabled: false, hostname: '', port: 14402 };

const getAggregatedState = (inputStates: InputStates | undefined, inputId: string | null): string | null => {
  if (!inputId || !inputStates || !inputStates[inputId]) {
    return null;
  }

  const nodeStates = Object.values(inputStates[inputId]);

  if (nodeStates.some((ns) => ns.state === 'RUNNING')) {
    return 'RUNNING';
  }

  if (nodeStates.some((ns) => ns.state === 'FAILED')) {
    return 'FAILED';
  }

  return nodeStates.length > 0 ? nodeStates[0].state : null;
};

const stateBsStyle = (state: string | null): 'success' | 'danger' | 'info' | 'warning' => {
  switch (state) {
    case 'RUNNING': return 'success';
    case 'FAILED': return 'danger';
    case 'STARTING': return 'info';
    default: return 'warning';
  }
};

const CollectorsSettings = () => {
  const { data: config, isLoading: isLoadingConfig } = useCollectorsConfig();
  const { updateConfig, isUpdatingConfig } = useCollectorsMutations();
  const isConfigured = !!config?.opamp_ca_id;
  const { data: inputStates } = useInputsStates({ enabled: isConfigured });

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

  const httpState = getAggregatedState(inputStates, config?.http?.input_id ?? null);
  const grpcState = getAggregatedState(inputStates, config?.grpc?.input_id ?? null);

  return (
    <>
      <Section>
        <h3>Ingest Endpoints</h3>

        {!isConfigured && (
          <Alert bsStyle="info">
            Certificate authority will be initialized on first save.
          </Alert>
        )}

        <h4>HTTP</h4>
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

        <h4>gRPC</h4>
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
      </Section>

      {isConfigured && (
        <Section>
          <h3>Ingest Endpoint Status</h3>
          {config.http.input_id && (
            <EndpointStatus>
              <strong>HTTP:</strong>
              <Badge bsStyle={stateBsStyle(httpState)}>{httpState ?? 'UNKNOWN'}</Badge>
              <Link to={Routes.SYSTEM.INPUT_DIAGNOSIS(config.http.input_id)}>View Diagnostics</Link>
            </EndpointStatus>
          )}
          {config.grpc.input_id && (
            <EndpointStatus>
              <strong>gRPC:</strong>
              <Badge bsStyle={stateBsStyle(grpcState)}>{grpcState ?? 'UNKNOWN'}</Badge>
              <Link to={Routes.SYSTEM.INPUT_DIAGNOSIS(config.grpc.input_id)}>View Diagnostics</Link>
            </EndpointStatus>
          )}
          {!config.http.input_id && !config.grpc.input_id && (
            <p>No ingest endpoints configured.</p>
          )}
        </Section>
      )}
    </>
  );
};

export default CollectorsSettings;
