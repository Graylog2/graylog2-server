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

import { Button, SegmentedControl } from 'components/bootstrap';
import { ClipboardButton, Select } from 'components/common';
import SectionGrid from 'components/common/Section/SectionGrid';

import { useFleets, useCollectorsMutations } from '../hooks';

type Platform = 'linux' | 'windows' | 'macos' | 'container';
type TokenExpiry = 'PT24H' | 'P7D' | 'P30D' | 'never';

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

const ScriptBlock = styled.pre(
  ({ theme }) => css`
    display: block;
    padding: ${theme.spacings.md};
    background: ${theme.colors.global.contentBackground};
    border: 1px solid ${theme.colors.gray[80]};
    border-radius: 4px;
    white-space: pre-wrap;
    word-break: break-all;
    font-family: ${theme.fonts.family.monospace};
    font-size: ${theme.fonts.size.small};
  `,
);

const CodeInline = styled.code(
  ({ theme }) => css`
    padding: 2px 6px;
    background: ${theme.colors.global.contentBackground};
    border: 1px solid ${theme.colors.gray[80]};
    border-radius: 4px;
    font-family: ${theme.fonts.family.monospace};
  `,
);

const TokenRow = styled.div`
  display: flex;
  align-items: center;
  gap: 0.5rem;
`;

const InfoText = styled.span(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.gray[60]};
    margin-top: ${theme.spacings.xs};
    display: block;
  `,
);

const ResultsContainer = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.lg};
  `,
);

const ResultSection = styled.div(
  ({ theme }) => css`
    h4 {
      margin: 0 0 ${theme.spacings.sm} 0;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: ${theme.fonts.size.large};
      font-weight: 500;
    }
  `,
);

type TokenResponse = {
  token: string;
  expiresAt: string;
};

const DeploymentForm = () => {
  const { data: fleets } = useFleets();
  const { createEnrollmentToken, isCreatingEnrollmentToken } = useCollectorsMutations();
  const [platform, setPlatform] = useState<Platform>('linux');
  const [fleetId, setFleetId] = useState<string | null>(null);
  const [expiry, setExpiry] = useState<TokenExpiry>('P7D');
  const [tokenResponse, setTokenResponse] = useState<TokenResponse | null>(null);

  const fleetOptions = (fleets || []).map((f) => ({ value: f.id, label: f.name }));

  const handleGenerate = async () => {
    if (!fleetId) return;

    const response = await createEnrollmentToken({
      fleetId,
      expiresIn: expiry === 'never' ? null : expiry,
    });

    setTokenResponse({
      token: response.token,
      expiresAt: response.expires_at,
    });
  };

  const getInstallScript = () => {
    if (!tokenResponse) return '';

    const { token } = tokenResponse;
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
          options={fleetOptions}
          value={fleetId}
          onChange={(selected) => setFleetId(selected as string)}
          clearable={false}
        />
      </Section>

      <Section>
        <Label>Token Expiry</Label>
        <SegmentedControl
          value={expiry}
          onChange={(v) => setExpiry(v as TokenExpiry)}
          data={[
            { value: 'PT24H', label: '24 hours' },
            { value: 'P7D', label: '7 days' },
            { value: 'P30D', label: '30 days' },
            { value: 'never', label: 'No expiry' },
          ]}
        />
      </Section>

      <Section>
        <Button
          bsStyle="primary"
          onClick={handleGenerate}
          disabled={!fleetId || isCreatingEnrollmentToken}
        >
          {isCreatingEnrollmentToken ? 'Generating...' : 'Generate Enrollment Token'}
        </Button>
      </Section>

      {tokenResponse && (
        <ResultsContainer>
          <SectionGrid>
            <ResultSection>
              <h4>
                Enrollment Token
                <ClipboardButton text={tokenResponse.token} title="Copy Token" bsSize="xs" />
              </h4>
              <TokenRow>
                <CodeInline style={{ flex: 1 }}>{tokenResponse.token.slice(0, 50)}...</CodeInline>
              </TokenRow>
              <InfoText>
                Fleet: {fleets?.find((f) => f.id === fleetId)?.name} | Expires:{' '}
                {new Date(tokenResponse.expiresAt).toLocaleString()}
              </InfoText>
            </ResultSection>

            <ResultSection>
              <h4>
                Installation Script
                <ClipboardButton text={getInstallScript()} title="Copy Script" bsSize="xs" />
              </h4>
              <ScriptBlock>{getInstallScript()}</ScriptBlock>
            </ResultSection>
          </SectionGrid>
        </ResultsContainer>
      )}
    </div>
  );
};

export default DeploymentForm;
