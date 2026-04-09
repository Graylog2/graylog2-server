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
import { useState, useCallback } from 'react';
import styled, { css } from 'styled-components';
import { Formik, Form } from 'formik';

import { SegmentedControl } from 'components/bootstrap';
import { ClipboardButton, FormikInput, Select } from 'components/common';
import SectionGrid from 'components/common/Section/SectionGrid';
import FormSubmit from 'components/common/FormSubmit';

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
    flex: 1;
    padding: 2px 6px;
    background: ${theme.colors.global.contentBackground};
    border: 1px solid ${theme.colors.gray[80]};
    border-radius: 4px;
    font-family: ${theme.fonts.family.monospace};
  `,
);

const TokenRow = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
  `,
);

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
  expiresAt: string | null;
};

type FormValues = {
  platform: Platform;
  fleetId: string;
  name: string;
  expiry: TokenExpiry;
};

const validate = (values: FormValues) => {
  const errors: Partial<Record<keyof FormValues, string>> = {};

  if (!values.fleetId) {
    errors.fleetId = 'Fleet is required';
  }

  if (!values.name.trim()) {
    errors.name = 'Name is required';
  }

  return errors;
};

const DeploymentForm = () => {
  const { data: fleets } = useFleets();
  const { createEnrollmentToken } = useCollectorsMutations();
  const [tokenResponse, setTokenResponse] = useState<TokenResponse | null>(null);

  const fleetOptions = (fleets || []).map((f) => ({ value: f.id, label: f.name }));

  const handleSubmit = useCallback(
    async (values: FormValues) => {
      try {
        const response = await createEnrollmentToken({
          name: values.name,
          fleetId: values.fleetId,
          expiresIn: values.expiry === 'never' ? null : values.expiry,
        });

        setTokenResponse({
          token: response.token,
          expiresAt: response.expires_at,
        });
      } catch {
        // Error notification handled by useCollectorsMutations onError callback
      }
    },
    [createEnrollmentToken],
  );

  const getInstallScript = (platform: Platform) => {
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

  const initialValues: FormValues = {
    platform: 'linux',
    fleetId: '',
    name: '',
    expiry: 'P7D',
  };

  return (
    <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate}>
      {({ isSubmitting, values, setFieldValue }) => (
        <Form>
          <Section>
            <Label>Platform</Label>
            <SegmentedControl
              value={values.platform}
              onChange={(v) => setFieldValue('platform', v)}
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
              value={values.fleetId}
              onChange={(selected) => setFieldValue('fleetId', selected as string)}
              clearable={false}
            />
          </Section>

          <Section>
            <FormikInput
              id="enrollment-token-name"
              type="text"
              label="Name *"
              name="name"
              placeholder="e.g. Initial Fleet Enrollment"
              required
            />
          </Section>

          <Section>
            <Label>Token Expiry</Label>
            <SegmentedControl
              value={values.expiry}
              onChange={(v) => setFieldValue('expiry', v)}
              data={[
                { value: 'PT24H', label: '24 hours' },
                { value: 'P7D', label: '7 days' },
                { value: 'P30D', label: '30 days' },
                { value: 'never', label: 'No expiry' },
              ]}
            />
          </Section>

          <FormSubmit
            isAsyncSubmit
            submitButtonText="Generate Enrollment Token"
            submitLoadingText="Generating..."
            isSubmitting={isSubmitting}
            displayCancel={false}
          />

          {tokenResponse && (
            <ResultsContainer>
              <SectionGrid>
                <ResultSection>
                  <h4>
                    Enrollment Token
                    <ClipboardButton text={tokenResponse.token} title="Copy Token" bsSize="xs" />
                  </h4>
                  <TokenRow>
                    <CodeInline>{tokenResponse.token.slice(0, 50)}...</CodeInline>
                  </TokenRow>
                  <InfoText>
                    Fleet: {fleets?.find((f) => f.id === values.fleetId)?.name} | Expires:{' '}
                    {tokenResponse.expiresAt ? new Date(tokenResponse.expiresAt).toLocaleString() : 'Never'}
                  </InfoText>
                </ResultSection>

                <ResultSection>
                  <h4>
                    Installation Script
                    <ClipboardButton text={getInstallScript(values.platform)} title="Copy Script" bsSize="xs" />
                  </h4>
                  <ScriptBlock>{getInstallScript(values.platform)}</ScriptBlock>
                </ResultSection>
              </SectionGrid>
            </ResultsContainer>
          )}
        </Form>
      )}
    </Formik>
  );
};

export default DeploymentForm;
