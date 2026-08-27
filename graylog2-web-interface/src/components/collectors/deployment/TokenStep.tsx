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

import { Button, Input, SegmentedControl } from 'components/bootstrap';
import { RelativeTime } from 'components/common';
import MutedText from 'components/collectors/common/MutedText';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import { useCollectorsMutations, useCollectorPermissions } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import type { Fleet } from '../types';

export type GeneratedToken = {
  token: string;
  name: string;
  // ISO-8601 duration the token is valid for, or null when it never expires.
  expiresIn: string | null;
  expiresAt: string | null;
};

type TokenExpiry = 'PT24H' | 'P7D' | 'P30D' | 'never';

type Props = {
  fleet: Fleet | null;
  generatedToken: GeneratedToken | null;
  onGenerated: (token: GeneratedToken) => void;
  onChangeToken: () => void;
};

const OptionsBox = styled.div(
  ({ theme }) => css`
    border: 1px solid ${theme.colors.cards.border};
    border-radius: ${theme.spacings.xs};
    padding: ${theme.spacings.md};

    /* Half the step width, matching the fleet step's box (FleetChoice inline). */
    max-width: 50%;
    margin-bottom: ${theme.spacings.sm};
  `,
);

const ModeRow = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.lg};
    flex-wrap: wrap;

    /* The radio inputs come wrapped in form-groups with their own bottom margin. */
    .form-group {
      margin-bottom: 0;
    }
  `,
);

const CustomFields = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.md};
    align-items: flex-end;
    flex-wrap: wrap;
    margin-top: ${theme.spacings.sm};

    /* The name input owns its own bottom margin via form-group; align the control with it. */
    > div:first-child {
      margin-bottom: ${theme.spacings.md};
    }

    /* The name input takes whatever width the fixed-size expiry control leaves over. */
    > .form-group {
      flex: 1;
      min-width: 200px;
    }
  `,
);

const SummaryBox = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: ${theme.spacings.md};
    border: 1px solid ${theme.colors.cards.border};
    border-radius: ${theme.spacings.xs};
    padding: ${theme.spacings.sm} ${theme.spacings.md};

    /* Half the step width, matching the fleet step's box (FleetChoice inline). */
    max-width: 50%;
  `,
);

const SHORT_LIVED_EXPIRY = 'P1D';
const SHORT_LIVED_NAME = 'deployment';

const TokenStep = ({ fleet, generatedToken, onGenerated, onChangeToken }: Props) => {
  const [mode, setMode] = useState<'short-lived' | 'custom'>('short-lived');
  // null = the user has not touched the field; the displayed name then follows the selected
  // fleet ("<fleet> rollout"). Anything the user typed (even an emptied field) wins.
  const [customName, setCustomName] = useState<string | null>(null);
  const [expiry, setExpiry] = useState<TokenExpiry>('P7D');
  const { createEnrollmentToken, isCreatingEnrollmentToken } = useCollectorsMutations();
  const { canCreateToken } = useCollectorPermissions();
  const sendTelemetry = useSendCollectorsTelemetry();

  const effectiveName = customName ?? (fleet ? `${fleet.name} rollout` : '');

  if (generatedToken) {
    return (
      <SummaryBox>
        <span>
          Token <strong>{generatedToken.name}</strong> generated
          {generatedToken.expiresAt ? (
            <>
              {' '}
              - expires <RelativeTime dateTime={generatedToken.expiresAt} />
            </>
          ) : (
            <> - never expires</>
          )}
          {fleet && (
            <>
              {' '}
              - scoped to fleet <strong>{fleet.name}</strong>
            </>
          )}
        </span>
        <Button onClick={onChangeToken}>Change token</Button>
      </SummaryBox>
    );
  }

  const handleGenerate = async () => {
    if (!fleet) return;

    const name = mode === 'custom' ? effectiveName.trim() : SHORT_LIVED_NAME;
    const customExpiresIn = expiry === 'never' ? null : expiry;
    const expiresIn = mode === 'custom' ? customExpiresIn : SHORT_LIVED_EXPIRY;

    try {
      const response = await createEnrollmentToken({ name, fleetId: fleet.id, expiresIn });

      sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.GENERATED, {
        app_action_value: 'deployment-generate',
        fleet_id: fleet.id,
        expires_in: expiresIn ?? 'never',
      });

      onGenerated({ token: response.token, name, expiresIn, expiresAt: response.expires_at });
    } catch {
      // Error notification handled by useCollectorsMutations onError callback
    }
  };

  const canGenerate = Boolean(fleet) && (mode === 'short-lived' || effectiveName.trim().length > 0);
  const isTokenCreationPermitted = Boolean(fleet) && canCreateToken(fleet.id);

  return (
    <div>
      <OptionsBox>
        <ModeRow>
          <Input
            type="radio"
            id="token-mode-short-lived"
            name="token-mode"
            label="Short-lived token (expires in 1 day)"
            checked={mode === 'short-lived'}
            onChange={() => setMode('short-lived')}
          />
          <Input
            type="radio"
            id="token-mode-custom"
            name="token-mode"
            label="Custom token"
            checked={mode === 'custom'}
            onChange={() => setMode('custom')}
          />
        </ModeRow>
        {mode === 'custom' && (
          <CustomFields>
            <div>
              <SegmentedControl
                value={expiry}
                onChange={(v) => {
                  sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.EXPIRY_SELECTED, {
                    app_action_value: 'deployment-expiry',
                    expires_in: v,
                  });
                  setExpiry(v as TokenExpiry);
                }}
                data={[
                  { value: 'PT24H', label: '24 hours' },
                  { value: 'P7D', label: '7 days' },
                  { value: 'P30D', label: '30 days' },
                  { value: 'never', label: 'No expiry' },
                ]}
              />
            </div>
            <Input
              type="text"
              id="custom-token-name"
              name="custom-token-name"
              label="Name"
              placeholder="e.g. web-servers rollout"
              value={effectiveName}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setCustomName(e.target.value)}
            />
          </CustomFields>
        )}
        <MutedText>Custom tokens appear on the Enrollment tokens tab and can be revoked any time.</MutedText>
      </OptionsBox>
      <Button
        bsStyle="primary"
        onClick={handleGenerate}
        disabled={!canGenerate || isCreatingEnrollmentToken || !isTokenCreationPermitted}
        title={
          fleet && !isTokenCreationPermitted
            ? 'You do not have permission to create enrollment tokens for this fleet'
            : undefined
        }>
        {isCreatingEnrollmentToken ? 'Generating...' : 'Generate token'}
      </Button>
      {!fleet && <MutedText>Select a fleet above first.</MutedText>}
    </div>
  );
};

export default TokenStep;
