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

import { Alert, SegmentedControl, HelpBlock } from 'components/bootstrap';
import FormSubmit from 'components/common/FormSubmit';
import { ClipboardButton, FormikInput, Select, Timestamp } from 'components/common';
import SectionGrid from 'components/common/Section/SectionGrid';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import { useFleets, useCollectorsMutations } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';

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
  fleetId: string;
  expiresAt: string | null;
};

type FormValues = {
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

const CollectorsDocsLink = () => (
  <a
    href="https://go2docs.graylog.org/current/getting_in_log_data/collectors.htm"
    target="_blank"
    rel="noopener noreferrer">
    Collector installation instructions
  </a>
);

const DeploymentForm = () => {
  const { data: fleets } = useFleets();
  const { createEnrollmentToken } = useCollectorsMutations();
  const sendTelemetry = useSendCollectorsTelemetry();
  const [tokenResponse, setTokenResponse] = useState<TokenResponse | null>(null);

  const fleetOptions = (fleets || []).map((f) => ({ value: f.id, label: f.name }));

  const handleSubmit = useCallback(
    async (values: FormValues, { resetForm }) => {
      try {
        const response = await createEnrollmentToken({
          name: values.name,
          fleetId: values.fleetId,
          expiresIn: values.expiry === 'never' ? null : values.expiry,
        });

        sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.GENERATED, {
          app_action_value: 'deployment-generate',
          fleet_id: values.fleetId,
          expires_in: values.expiry,
        });

        setTokenResponse({
          token: response.token,
          fleetId: response.fleet_id,
          expiresAt: response.expires_at,
        });

        resetForm();
      } catch {
        // Error notification handled by useCollectorsMutations onError callback
      }
    },
    [createEnrollmentToken, sendTelemetry],
  );

  const initialValues: FormValues = {
    fleetId: '',
    name: '',
    expiry: 'P7D',
  };

  return (
    <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate}>
      {({ isSubmitting, values, setFieldValue }) => (
        <Form>
          <Alert bsStyle="info">
            <strong>How deployment works:</strong> Select a fleet and generate an enrollment token. Then follow the{' '}
            <CollectorsDocsLink /> to install the Collector on your target host &mdash; it will enroll, receive its
            fleet&apos;s configuration, and start collecting data automatically.
          </Alert>

          <Section>
            <Label>
              <strong>Fleet *</strong>
            </Label>
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
            <HelpBlock>Collectors enrolled with this token will be assigned to the selected fleet.</HelpBlock>
          </Section>

          <Section>
            <FormikInput
              id="enrollment-token-name"
              type="text"
              label="Name *"
              name="name"
              placeholder="e.g. Initial Fleet Enrollment"
              help="A descriptive token name to simplify token management."
              required
            />
          </Section>

          <Section>
            <Label>
              <strong>Token Expiry *</strong>
            </Label>
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
            <HelpBlock>
              How long this token remains valid for new enrollments. Already-enrolled Collectors are not affected when a
              token expires.
            </HelpBlock>
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
                  <HelpBlock>
                    <ul style={{ paddingLeft: 0 }}>
                      <li>
                        <strong>Fleet:</strong> {fleets?.find((f) => f.id === tokenResponse.fleetId)?.name}
                      </li>
                      <li>
                        <strong>Expires:</strong>{' '}
                        {tokenResponse.expiresAt ? <Timestamp dateTime={tokenResponse.expiresAt} /> : 'Never'}
                      </li>
                    </ul>
                  </HelpBlock>
                </ResultSection>

                <ResultSection>
                  <h4>Installation</h4>
                  <p>
                    Copy the enrollment token above, then follow the <CollectorsDocsLink /> to install and enroll the
                    Collector on your target host(s).
                  </p>
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
