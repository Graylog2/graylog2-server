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
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import { HoverForHelp } from 'components/common';

import type { FleetStatsSummary } from '../types';

export type HealthStatus = 'healthy' | 'degraded' | 'down' | 'empty';

export const getHealthStatus = (stats: FleetStatsSummary): HealthStatus => {
  if (stats.total_instances === 0) return 'empty';
  if (stats.online_instances === stats.total_instances) return 'healthy';
  if (stats.online_instances === 0) return 'down';

  return 'degraded';
};

const HEALTH_VARIANT: Record<HealthStatus, string> = {
  healthy: 'success',
  degraded: 'warning',
  down: 'danger',
  empty: '',
};

const Card = styled.div<{ $status: HealthStatus }>(({ theme, $status }) => {
  const bandColor = HEALTH_VARIANT[$status]
    ? theme.colors.variant[HEALTH_VARIANT[$status] as keyof typeof theme.colors.variant]
    : theme.colors.gray[60];

  return css`
    position: relative;
    display: flex;
    flex-direction: column;
    border: 1px solid ${theme.colors.gray[80]};
    border-radius: 8px;
    padding: ${theme.spacings.md};
    background: ${theme.colors.global.contentBackground};
    cursor: pointer;
    border-left: 3px solid ${bandColor};

    &:hover {
      outline: 1px solid ${theme.colors.variant.info};
    }
  `;
});

const HelpCorner = styled.div(
  ({ theme }) => css`
    position: absolute;
    top: ${theme.spacings.xs};
    right: ${theme.spacings.xs};
    color: ${theme.colors.gray[70]};
    font-size: 0.7em;
  `,
);

const FleetName = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.variant.info};
    font-weight: 600;
    font-size: ${theme.fonts.size.large};
  `,
);

const HeroNumber = styled.div(
  ({ theme }) => css`
    font-size: 28px;
    font-weight: 700;
    margin: ${theme.spacings.xs} 0 2px;
  `,
);

const SubLabel = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.text.secondary};
  `,
);

const Breakdown = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.sm};
    margin-top: ${theme.spacings.xs};
    min-height: 1.4em;
    font-size: ${theme.fonts.size.small};
    flex: 1;
  `,
);

const OnlineCount = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.variant.success};
  `,
);

const OfflineCount = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.variant.warning};
  `,
);

const SourceFooter = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.small};
    color: ${theme.colors.text.secondary};
    margin-top: ${theme.spacings.xs};
    border-top: 1px solid ${theme.colors.gray[90]};
    padding-top: ${theme.spacings.xs};
  `,
);

type Props = {
  stats: FleetStatsSummary;
  onClick: () => void;
};

const HEALTH_TOOLTIP_KEYS: Record<HealthStatus, string> = {
  healthy: 'fleet_card.healthy',
  degraded: 'fleet_card.degraded',
  down: 'fleet_card.down',
  empty: 'fleet_card.empty',
};

const FleetCard = ({ stats, onClick }: Props) => {
  const { t } = useTranslation('collectors');
  const health = getHealthStatus(stats);

  return (
    <Card onClick={onClick} data-testid="fleet-card" $status={health}>
      <HelpCorner>
        <HoverForHelp title={stats.fleet_name} placement="right" pullRight={false}>
          {t(HEALTH_TOOLTIP_KEYS[health])}
        </HoverForHelp>
      </HelpCorner>
      <FleetName>{stats.fleet_name}</FleetName>
      <HeroNumber>{stats.total_instances}</HeroNumber>
      <SubLabel>{t('fleet_card.instances', { count: stats.total_instances })}</SubLabel>
      <Breakdown>
        {stats.online_instances > 0 && (
          <OnlineCount>{t('fleet_card.online', { count: stats.online_instances })}</OnlineCount>
        )}
        {stats.offline_instances > 0 && (
          <OfflineCount>{t('fleet_card.offline', { count: stats.offline_instances })}</OfflineCount>
        )}
      </Breakdown>
      <SourceFooter>{t('fleet_card.sources', { count: stats.total_sources })}</SourceFooter>
    </Card>
  );
};

export default FleetCard;
