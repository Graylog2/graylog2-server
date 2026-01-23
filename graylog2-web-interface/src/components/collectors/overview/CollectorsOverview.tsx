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
import { Flex, Title } from '@mantine/core';

import { Spinner } from 'components/common';

import { useCollectorStats, useFleets, useSources } from '../hooks';
import StatCard from '../common/StatCard';

import SourcesTable from './SourcesTable';

const StatsRow = styled(Flex)(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.lg};
    gap: ${theme.spacings.md};
    flex-wrap: wrap;
  `,
);

const Section = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.lg};
  `,
);

const CollectorsOverview = () => {
  const { data: stats, isLoading: statsLoading } = useCollectorStats();
  const { data: fleets, isLoading: fleetsLoading } = useFleets();
  const { data: sources, isLoading: sourcesLoading } = useSources();

  const isLoading = statsLoading || fleetsLoading || sourcesLoading;

  if (isLoading) {
    return <Spinner />;
  }

  const fleetNames = (fleets || []).reduce(
    (acc, fleet) => ({ ...acc, [fleet.id]: fleet.name }),
    {} as Record<string, string>,
  );

  return (
    <div>
      <StatsRow>
        <StatCard value={stats?.total_instances || 0} label="Instances" />
        <StatCard value={stats?.online_instances || 0} label="Online" variant="success" />
        <StatCard value={stats?.offline_instances || 0} label="Offline" variant="warning" />
        <StatCard value={stats?.total_fleets || 0} label="Fleets" />
        <StatCard value={stats?.total_sources || 0} label="Sources" />
      </StatsRow>

      <Section>
        <Title order={4}>Active Sources</Title>
        <SourcesTable sources={sources || []} fleetNames={fleetNames} />
      </Section>
    </div>
  );
};

export default CollectorsOverview;
