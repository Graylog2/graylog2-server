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

import FleetCardsGrid from './FleetCardsGrid';
import RecentActivity from './RecentActivity';

import { useCollectorStats, useFleetsBulkStats } from '../hooks';
import StatCard from '../common/StatCard';

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

const CollectorsOverview = () => {
  const { data: stats, isLoading: statsLoading, isError: statsError } = useCollectorStats();
  const { data: bulkStats, isLoading: bulkLoading, isError: bulkError } = useFleetsBulkStats();
  const [filter, setFilter] = useState('');
  const history = useHistory();

  return (
    <div>
      {statsLoading ? (
        <Spinner />
      ) : statsError ? (
        <Alert bsStyle="danger">Could not load collector stats.</Alert>
      ) : (
        <StatsRow>
          <StatCard
            value={stats?.total_instances ?? 0}
            label="Instances"
            onClick={() => history.push(Routes.SYSTEM.COLLECTORS.INSTANCES)}
          />
          <StatCard
            value={stats?.online_instances ?? 0}
            label="Online"
            variant="success"
            onClick={() => history.push(`${Routes.SYSTEM.COLLECTORS.INSTANCES}?filters=status%3Donline`)}
          />
          <StatCard
            value={stats?.offline_instances ?? 0}
            label="Offline"
            variant="warning"
            onClick={() => history.push(`${Routes.SYSTEM.COLLECTORS.INSTANCES}?filters=status%3Doffline`)}
          />
          <StatCard
            value={stats?.total_fleets ?? 0}
            label="Fleets"
            onClick={() => history.push(Routes.SYSTEM.COLLECTORS.FLEETS)}
          />
          <StatCard value={stats?.total_sources ?? 0} label="Sources" />
        </StatsRow>
      )}

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

      {bulkLoading ? (
        <Spinner />
      ) : bulkError ? (
        <Alert bsStyle="danger">Could not load fleet stats.</Alert>
      ) : (
        <FleetCardsGrid fleets={bulkStats?.fleets || []} filter={filter} />
      )}

      <RecentActivity />
    </div>
  );
};

export default CollectorsOverview;
