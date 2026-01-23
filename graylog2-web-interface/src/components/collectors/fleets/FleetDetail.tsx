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
import { Tabs, Flex, Badge } from '@mantine/core';

import { Spinner } from 'components/common';

import { useFleet, useFleetStats, useInstances, useSources } from '../hooks';
import StatCard from '../common/StatCard';
import SourcesTable from '../overview/SourcesTable';

type Props = {
  fleetId: string;
};

const Header = styled(Flex)(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.lg};
    gap: ${theme.spacings.md};
    align-items: center;
  `,
);

const StatsRow = styled(Flex)(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.lg};
    gap: ${theme.spacings.sm};
  `,
);

const FleetDetail = ({ fleetId }: Props) => {
  const { data: fleet, isLoading: fleetLoading } = useFleet(fleetId);
  const { data: stats, isLoading: statsLoading } = useFleetStats(fleetId);
  const { data: instances } = useInstances(fleetId);
  const { data: sources } = useSources(fleetId);

  if (fleetLoading || statsLoading) {
    return <Spinner />;
  }

  if (!fleet) {
    return <div>Fleet not found</div>;
  }

  const fleetNames = { [fleet.id]: fleet.name };

  return (
    <div>
      <Header>
        <h2>{fleet.name}</h2>
        {fleet.target_version && <Badge>v{fleet.target_version}</Badge>}
      </Header>

      <StatsRow>
        <StatCard value={stats?.total_instances || 0} label="Instances" />
        <StatCard value={stats?.online_instances || 0} label="Online" variant="success" />
        <StatCard value={stats?.offline_instances || 0} label="Offline" variant="warning" />
        <StatCard value={stats?.total_sources || 0} label="Sources" />
      </StatsRow>

      <Tabs defaultValue="sources">
        <Tabs.List>
          <Tabs.Tab value="sources">Sources</Tabs.Tab>
          <Tabs.Tab value="instances">Instances ({instances?.length || 0})</Tabs.Tab>
          <Tabs.Tab value="settings">Settings</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="sources" pt="md">
          <SourcesTable sources={sources || []} fleetNames={fleetNames} />
        </Tabs.Panel>

        <Tabs.Panel value="instances" pt="md">
          <p>Instance list coming in next task...</p>
        </Tabs.Panel>

        <Tabs.Panel value="settings" pt="md">
          <p>Fleet settings coming soon...</p>
        </Tabs.Panel>
      </Tabs>
    </div>
  );
};

export default FleetDetail;
