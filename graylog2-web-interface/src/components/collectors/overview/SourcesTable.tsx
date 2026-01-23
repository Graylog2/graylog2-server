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
import { Table, Badge } from '@mantine/core';

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

import type { Source, SourceType } from '../types';

type Props = {
  sources: Source[];
  fleetNames: Record<string, string>;
};

const sourceTypeLabels: Record<SourceType, string> = {
  file: 'File',
  journald: 'Journald',
  windows_event_log: 'WinEventLog',
  tcp: 'TCP',
  udp: 'UDP',
};

const StyledTable = styled(Table)`
  margin-top: 1rem;
`;

const EmptyState = styled.div`
  padding: 2rem;
  text-align: center;
  color: ${({ theme }) => theme.colors.gray[60]};
`;

const SourcesTable = ({ sources, fleetNames }: Props) => {
  if (sources.length === 0) {
    return <EmptyState>No sources configured yet.</EmptyState>;
  }

  return (
    <StyledTable striped highlightOnHover>
      <Table.Thead>
        <Table.Tr>
          <Table.Th>Source Type</Table.Th>
          <Table.Th>Name</Table.Th>
          <Table.Th>Fleet</Table.Th>
          <Table.Th>Status</Table.Th>
        </Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {sources.map((source) => (
          <Table.Tr key={source.id}>
            <Table.Td>
              <Badge variant="light">{sourceTypeLabels[source.type]}</Badge>
            </Table.Td>
            <Table.Td>{source.name}</Table.Td>
            <Table.Td>
              <Link to={Routes.SYSTEM.COLLECTORS.FLEET(source.fleet_id)}>
                {fleetNames[source.fleet_id] || source.fleet_id}
              </Link>
            </Table.Td>
            <Table.Td>
              <Badge color={source.enabled ? 'green' : 'gray'}>
                {source.enabled ? 'Enabled' : 'Disabled'}
              </Badge>
            </Table.Td>
          </Table.Tr>
        ))}
      </Table.Tbody>
    </StyledTable>
  );
};

export default SourcesTable;
