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

import { useState } from 'react';
import styled, { css } from 'styled-components';
import {
  SegmentedControl,
  Select,
  Radio,
  Button,
  Code,
  CopyButton,
  Group,
  Stack,
  Card,
  Text,
} from '@mantine/core';

import { useFleets } from '../hooks';

type Platform = 'linux' | 'windows' | 'macos' | 'container';
type TokenExpiry = '24h' | '7d' | '30d' | 'never';

const Section = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.lg};
  `,
);

const Label = styled.label(
  ({ theme }) => css`
    display: block;
    font-weight: 500;
    margin-bottom: ${theme.spacings.xs};
  `,
);

const ScriptBlock = styled(Code)(
  ({ theme }) => css`
    display: block;
    padding: ${theme.spacings.md};
    white-space: pre-wrap;
    word-break: break-all;
  `,
);

const generateMockToken = () =>
  `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.${btoa(JSON.stringify({ fleet: 'test', exp: Date.now() + 86400000 }))}.mock-signature`;

const DeploymentForm = () => {
  const { data: fleets } = useFleets();
  const [platform, setPlatform] = useState<Platform>('linux');
  const [fleetId, setFleetId] = useState<string | null>(null);
  const [expiry, setExpiry] = useState<TokenExpiry>('24h');
  const [token, setToken] = useState<string | null>(null);

  const fleetOptions = (fleets || []).map((f) => ({ value: f.id, label: f.name }));

  const handleGenerate = () => {
    setToken(generateMockToken());
  };

  const getInstallScript = () => {
    if (!token) return '';

    const scripts: Record<Platform, string> = {
      linux: `curl -sL https://graylog.example.com/collector/install.sh \\
  | sudo bash -s -- \\
  --token "${token}"`,
      windows: `Invoke-WebRequest -Uri https://graylog.example.com/collector/install.ps1 -OutFile install.ps1
.\\install.ps1 -Token "${token}"`,
      macos: `curl -sL https://graylog.example.com/collector/install.sh \\
  | sudo bash -s -- \\
  --token "${token}"`,
      container: `docker run -d \\
  -e GRAYLOG_TOKEN="${token}" \\
  graylog/collector:latest`,
    };

    return scripts[platform];
  };

  return (
    <div>
      <Section>
        <Label>Platform</Label>
        <SegmentedControl
          value={platform}
          onChange={(v) => setPlatform(v as Platform)}
          data={[
            { value: 'linux', label: 'Linux' },
            { value: 'windows', label: 'Windows' },
            { value: 'macos', label: 'macOS' },
            { value: 'container', label: 'Container' },
          ]}
        />
      </Section>

      <Section>
        <Label>Fleet *</Label>
        <Select
          placeholder="Select a fleet"
          data={fleetOptions}
          value={fleetId}
          onChange={setFleetId}
          searchable
        />
      </Section>

      <Section>
        <Label>Token Expiry</Label>
        <Radio.Group value={expiry} onChange={(v) => setExpiry(v as TokenExpiry)}>
          <Group>
            <Radio value="24h" label="24 hours" />
            <Radio value="7d" label="7 days" />
            <Radio value="30d" label="30 days" />
            <Radio value="never" label="No expiry" />
          </Group>
        </Radio.Group>
      </Section>

      <Section>
        <Button onClick={handleGenerate} disabled={!fleetId}>
          Generate Enrollment Token
        </Button>
      </Section>

      {token && (
        <Stack gap="md">
          <Card withBorder>
            <Label>Enrollment Token</Label>
            <Group>
              <Code style={{ flex: 1 }}>{token.slice(0, 50)}...</Code>
              <CopyButton value={token}>
                {({ copied, copy }) => (
                  <Button variant="light" size="xs" onClick={copy}>
                    {copied ? 'Copied!' : 'Copy'}
                  </Button>
                )}
              </CopyButton>
            </Group>
            <Text size="sm" c="dimmed" mt="xs">
              Fleet: {fleets?.find((f) => f.id === fleetId)?.name} | Expires:{' '}
              {expiry === 'never' ? 'Never' : expiry}
            </Text>
          </Card>

          <Card withBorder>
            <Group justify="space-between" mb="sm">
              <Label>Installation Script</Label>
              <CopyButton value={getInstallScript()}>
                {({ copied, copy }) => (
                  <Button variant="light" size="xs" onClick={copy}>
                    {copied ? 'Copied!' : 'Copy Script'}
                  </Button>
                )}
              </CopyButton>
            </Group>
            <ScriptBlock>{getInstallScript()}</ScriptBlock>
          </Card>
        </Stack>
      )}
    </div>
  );
};

export default DeploymentForm;
