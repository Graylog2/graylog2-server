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
import { Trans, useTranslation } from 'react-i18next';

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
  const { t } = useTranslation('collectors');
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
          <Trans
            t={t}
            i18nKey={[
              'token_step.summary',
              generatedToken.expiresAt ? '_expires' : '_never_expires',
              fleet ? '_with_fleet' : '',
            ].join('')}
            values={{ tokenName: generatedToken.name, fleetName: fleet?.name }}
            components={{
              emphasis: <strong />,
              expiresAt: <RelativeTime dateTime={generatedToken.expiresAt} />,
            }}
          />
        </span>
        <Button
          onClick={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.CHANGE_CLICKED, {
              app_action_value: 'deployment-change-token',
            });

            onChangeToken();
          }}>
          {t('token_step.change_token')}
        </Button>
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
        mode,
        expires_in: expiresIn ?? 'never',
      });

      onGenerated({ token: response.token, name, expiresIn, expiresAt: response.expires_at });
    } catch {
      // Error notification handled by useCollectorsMutations onError callback
      sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.GENERATE_FAILED, {
        app_action_value: 'deployment-generate-failed',
        fleet_id: fleet.id,
        mode,
      });
    }
  };

  const canGenerate = Boolean(fleet) && (mode === 'short-lived' || effectiveName.trim().length > 0);
  const isTokenCreationPermitted = Boolean(fleet) && canCreateToken(fleet.id);

  const handleModeChange = (newMode: 'short-lived' | 'custom') => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.ENROLLMENT_TOKEN.MODE_SELECTED, {
      app_action_value: 'deployment-token-mode',
      mode: newMode,
    });

    setMode(newMode);
  };

  return (
    <div>
      <OptionsBox>
        <ModeRow>
          <Input
            type="radio"
            id="token-mode-short-lived"
            name="token-mode"
            label={t('token_step.short_lived')}
            checked={mode === 'short-lived'}
            onChange={() => handleModeChange('short-lived')}
          />
          <Input
            type="radio"
            id="token-mode-custom"
            name="token-mode"
            label={t('token_step.custom')}
            checked={mode === 'custom'}
            onChange={() => handleModeChange('custom')}
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
                  { value: 'PT24H', label: t('token_step.expiry_24_hours') },
                  { value: 'P7D', label: t('token_step.expiry_7_days') },
                  { value: 'P30D', label: t('token_step.expiry_30_days') },
                  { value: 'never', label: t('token_step.expiry_never') },
                ]}
              />
            </div>
            <Input
              type="text"
              id="custom-token-name"
              name="custom-token-name"
              label={t('token_step.name')}
              placeholder={t('token_step.name_placeholder')}
              value={effectiveName}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setCustomName(e.target.value)}
            />
          </CustomFields>
        )}
        <MutedText>{t('token_step.custom_hint')}</MutedText>
      </OptionsBox>
      <Button
        bsStyle="primary"
        onClick={handleGenerate}
        disabled={!canGenerate || isCreatingEnrollmentToken || !isTokenCreationPermitted}
        title={fleet && !isTokenCreationPermitted ? t('token_step.no_permission') : undefined}>
        {isCreatingEnrollmentToken ? t('token_step.generating') : t('token_step.generate')}
      </Button>
      {!fleet && <MutedText>{t('token_step.select_fleet_first')}</MutedText>}
    </div>
  );
};

export default TokenStep;
