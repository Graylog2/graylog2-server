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

import type { FleetStatsSummary } from '../types';

type HealthStatus = 'healthy' | 'degraded' | 'down' | 'empty';

const getHealthStatus = (stats: FleetStatsSummary): HealthStatus => {
  if (stats.total_instances === 0) return 'empty';
  if (stats.online_instances === stats.total_instances) return 'healthy';
  if (stats.online_instances === 0) return 'down';
  return 'degraded';
};

const Card = styled.div(
  ({ theme }) => css`
    border: 1px solid ${theme.colors.gray[80]};
    border-radius: 8px;
    padding: ${theme.spacings.md};
    background: ${theme.colors.global.contentBackground};
    cursor: pointer;

    &:hover {
      outline: 1px solid ${theme.colors.variant.info};
    }
  `,
);

const CardHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

const FleetName = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.variant.info};
    font-weight: 600;
    font-size: ${theme.fonts.size.large};
  `,
);

const HealthDot = styled.span<{ $status: HealthStatus }>(({ theme, $status }) => {
  const colorMap: Record<HealthStatus, string> = {
    healthy: theme.colors.variant.success,
    degraded: theme.colors.variant.warning,
    down: theme.colors.variant.danger,
    empty: theme.colors.gray[60],
  };

  return css`
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: ${colorMap[$status]};
  `;
});

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
    color: ${theme.colors.gray[60]};
  `,
);

const Breakdown = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.sm};
    margin-top: ${theme.spacings.xs};
    font-size: ${theme.fonts.size.small};
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
    color: ${theme.colors.gray[60]};
    margin-top: ${theme.spacings.xs};
    border-top: 1px solid ${theme.colors.gray[90]};
    padding-top: ${theme.spacings.xs};
  `,
);

type Props = {
  stats: FleetStatsSummary;
  onClick: () => void;
};

const FleetCard = ({ stats, onClick }: Props) => {
  const health = getHealthStatus(stats);

  return (
    <Card onClick={onClick} data-testid="fleet-card">
      <CardHeader>
        <FleetName>{stats.fleet_name}</FleetName>
        <HealthDot $status={health} title={health} />
      </CardHeader>
      <HeroNumber>{stats.total_instances}</HeroNumber>
      <SubLabel>instances</SubLabel>
      <Breakdown>
        {stats.online_instances > 0 && <OnlineCount>● {stats.online_instances} online</OnlineCount>}
        {stats.offline_instances > 0 && <OfflineCount>● {stats.offline_instances} offline</OfflineCount>}
      </Breakdown>
      <SourceFooter>
        {stats.total_sources} {stats.total_sources === 1 ? 'source' : 'sources'}
      </SourceFooter>
    </Card>
  );
};

export default FleetCard;
