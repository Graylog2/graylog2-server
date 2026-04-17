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

import { Alert, Input } from 'components/bootstrap';
import { Spinner } from 'components/common';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import FleetCardsGrid from './FleetCardsGrid';
import RecentActivity from './RecentActivity';

import { useCollectorStats, useFleetsBulkStats } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import StatCard, { type Variant as StatCardVariant } from '../common/StatCard';

const StatsRow = styled.div(
  ({ theme }) => css`
    display: flex;
    margin-bottom: ${theme.spacings.lg};
    gap: ${theme.spacings.md};
    flex-wrap: wrap;
  `,
);

const SectionHeader = styled.div(
  ({ theme }) => css`
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: ${theme.spacings.sm};
  `,
);

const SectionTitle = styled.h3(
  ({ theme }) => css`
    margin: 0;
    font-size: ${theme.fonts.size.h3};
  `,
);

const StatsSection = () => {
  const { data: stats, isLoading, isError } = useCollectorStats();
  const history = useHistory();
  const sendTelemetry = useSendCollectorsTelemetry();

  const snapshot = {
    total_instances: stats?.total_instances ?? 0,
    online_instances: stats?.online_instances ?? 0,
    offline_instances: stats?.offline_instances ?? 0,
    total_fleets: stats?.total_fleets ?? 0,
    total_sources: stats?.total_sources ?? 0,
  };

  const emitStatCard = (
    card: 'instances' | 'online' | 'offline' | 'fleets',
    value: number,
    variant: StatCardVariant,
    navigates_to: 'instances' | 'instances-online' | 'instances-offline' | 'fleets',
  ) => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.OVERVIEW.STAT_CARD_CLICKED, {
      app_action_value: `stat-card-${card}`,
      card,
      value,
      variant,
      navigates_to,
      ...snapshot,
    });
  };

  if (isLoading) return <Spinner />;

  if (isError) return <Alert bsStyle="danger">Could not load collector stats.</Alert>;

  return (
    <StatsRow>
      <StatCard
        value={stats?.total_instances ?? 0}
        label="Instances"
        helpText="Running collector processes across all fleets."
        onClick={() => {
          emitStatCard('instances', stats?.total_instances ?? 0, 'default', 'instances');
          history.push(Routes.SYSTEM.COLLECTORS.INSTANCES);
        }}
      />
      <StatCard
        value={stats?.online_instances ?? 0}
        label="Online"
        helpText="Instances that reported a heartbeat within the offline threshold."
        variant="success"
        onClick={() => {
          emitStatCard('online', stats?.online_instances ?? 0, 'success', 'instances-online');
          history.push(`${Routes.SYSTEM.COLLECTORS.INSTANCES}?filters=status%3Donline`);
        }}
      />
      <StatCard
        value={stats?.offline_instances ?? 0}
        label="Offline"
        helpText="Instances that missed their heartbeat. Check host connectivity or collector process status."
        variant="warning"
        onClick={() => {
          emitStatCard('offline', stats?.offline_instances ?? 0, 'warning', 'instances-offline');
          history.push(`${Routes.SYSTEM.COLLECTORS.INSTANCES}?filters=status%3Doffline`);
        }}
      />
      <StatCard
        value={stats?.total_fleets ?? 0}
        label="Fleets"
        helpText="Logical groups of collectors that share the same source configuration."
        onClick={() => {
          emitStatCard('fleets', stats?.total_fleets ?? 0, 'default', 'fleets');
          history.push(Routes.SYSTEM.COLLECTORS.FLEETS);
        }}
      />
      <StatCard
        value={stats?.total_sources ?? 0}
        label="Sources"
        helpText="Data collection configurations (file paths, journald, Windows Event Logs) across all fleets."
      />
    </StatsRow>
  );
};

const FleetsSection = ({ filter }: { filter: string }) => {
  const { data: bulkStats, isLoading, isError } = useFleetsBulkStats();

  if (isLoading) return <Spinner />;

  if (isError) return <Alert bsStyle="danger">Could not load fleet stats.</Alert>;

  return <FleetCardsGrid fleets={bulkStats?.fleets || []} filter={filter} />;
};

const CollectorsOverview = () => {
  const [filter, setFilter] = useState('');

  return (
    <div>
      <StatsSection />

      <SectionHeader>
        <SectionTitle>Fleets</SectionTitle>
        <Input
          type="text"
          placeholder="Filter fleets..."
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          style={{ width: 200, marginBottom: 0 }}
        />
      </SectionHeader>

      <FleetsSection filter={filter} />

      <RecentActivity />
    </div>
  );
};

export default CollectorsOverview;
