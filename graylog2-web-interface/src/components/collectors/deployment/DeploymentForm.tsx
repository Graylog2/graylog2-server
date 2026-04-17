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

import { Alert, SegmentedControl } from 'components/bootstrap';
import FormSubmit from 'components/common/FormSubmit';
import { ClipboardButton, FormikInput, Select } from 'components/common';
import SectionGrid from 'components/common/Section/SectionGrid';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import { useFleets, useCollectorsMutations } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';

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
  const sendTelemetry = useSendCollectorsTelemetry();
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

        sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.GENERATED, {
          app_action_value: 'deployment-generate',
          fleet_id: values.fleetId,
          platform: values.platform,
          expires_in: values.expiry,
        });

        setTokenResponse({
          token: response.token,
          expiresAt: response.expires_at,
        });
      } catch {
        // Error notification handled by useCollectorsMutations onError callback
      }
    },
    [createEnrollmentToken, sendTelemetry],
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
          <Alert bsStyle="info">
            <strong>How deployment works:</strong> Select a target platform and fleet, then generate an enrollment
            token. Run the installation script on your target host &mdash; the collector will enroll, receive its
            fleet&apos;s configuration, and start collecting data automatically.
          </Alert>
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
              onChange={(selected) => {
                const newFleetId = selected as string;
                sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.FLEET_SELECTED, {
                  app_action_value: 'deployment-fleet',
                  fleet_id: newFleetId,
                });
                setFieldValue('fleetId', newFleetId);
              }}
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
            <InfoText>
              How long this token remains valid for new enrollments. Already-enrolled collectors are not affected when a
              token expires.
            </InfoText>
            <SegmentedControl
              value={values.expiry}
              onChange={(v) => {
                sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.EXPIRY_SELECTED, {
                  app_action_value: 'deployment-expiry',
                  expires_in: v,
                });
                setFieldValue('expiry', v);
              }}
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
                    <ClipboardButton
                      text={tokenResponse.token}
                      title="Copy Token"
                      bsSize="xs"
                      onSuccess={() =>
                        sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.TOKEN_COPIED, {
                          app_action_value: 'deployment-copy-token',
                        })
                      }
                    />
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
                  <InfoText>
                    Run this script on the target host. The collector will download, install, and enroll automatically.
                  </InfoText>
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
