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
import styled from 'styled-components';
import { Table, Badge, Button } from '@mantine/core';

import { Link } from 'components/common/router';
import { RelativeTime } from 'components/common';
import Routes from 'routing/Routes';

import type { Fleet } from '../types';
import { useFleetStats } from '../hooks';

type Props = {
  fleets: Fleet[];
};

const EmptyState = styled.div`
  padding: 2rem;
  text-align: center;
  color: ${({ theme }) => theme.colors.gray[60]};
`;

const FleetRow = ({ fleet }: { fleet: Fleet }) => {
  const { data: stats } = useFleetStats(fleet.id);

  const statusColor = stats && stats.offline_instances > 0 ? 'yellow' : 'green';
  const statusText = stats && stats.offline_instances > 0 ? 'Degraded' : 'Healthy';

  return (
    <Table.Tr>
      <Table.Td>
        <Link to={Routes.SYSTEM.COLLECTORS.FLEET(fleet.id)}>{fleet.name}</Link>
      </Table.Td>
      <Table.Td>{fleet.description}</Table.Td>
      <Table.Td>{stats ? `${stats.online_instances} online` : '-'}</Table.Td>
      <Table.Td>{stats?.total_sources || 0}</Table.Td>
      <Table.Td>
        <Badge color={statusColor}>{statusText}</Badge>
      </Table.Td>
      <Table.Td>
        <RelativeTime dateTime={fleet.updated_at} />
      </Table.Td>
    </Table.Tr>
  );
};

const FleetList = ({ fleets }: Props) => {
  if (fleets.length === 0) {
    return (
      <EmptyState>
        <p>No fleets configured yet.</p>
        <Button>Create Fleet</Button>
      </EmptyState>
    );
  }

  return (
    <Table striped highlightOnHover>
      <Table.Thead>
        <Table.Tr>
          <Table.Th>Name</Table.Th>
          <Table.Th>Description</Table.Th>
          <Table.Th>Instances</Table.Th>
          <Table.Th>Sources</Table.Th>
          <Table.Th>Status</Table.Th>
          <Table.Th>Updated</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {fleets.map((fleet) => (
          <FleetRow key={fleet.id} fleet={fleet} />
        ))}
      </Table.Tbody>
    </Table>
  );
};

export default FleetList;
