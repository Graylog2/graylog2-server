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
import styled from 'styled-components';
import { Table, Badge, Flex } from '@mantine/core';

import { Link } from 'components/common/router';
import { RelativeTime } from 'components/common';
import Routes from 'routing/Routes';

import InstanceDetailDrawer from './InstanceDetailDrawer';

import type { CollectorInstanceView, Source } from '../types';
import StatCard from '../common/StatCard';

type Props = {
  instances: CollectorInstanceView[];
  fleetNames: Record<string, string>;
  sources?: Source[];
  showStats?: boolean;
};

const EmptyState = styled.div`
  padding: 2rem;
  text-align: center;
  color: ${({ theme }) => theme.colors.gray[60]};
`;

const StatsRow = styled(Flex)`
  margin-bottom: 1rem;
  gap: 0.5rem;
`;

const ClickableRow = styled(Table.Tr)`
  cursor: pointer;

  &:hover {
    background-color: ${({ theme }) => theme.colors.gray[90]};
  }
`;

const OsIcon = ({ os }: { os: string | null }) => {
  if (os === 'linux') return <span title="Linux">🐧</span>;
  if (os === 'windows') return <span title="Windows">🪟</span>;
  if (os === 'darwin') return <span title="macOS">🍎</span>;

  return <span>❓</span>;
};

const InstanceList = ({ instances, fleetNames, sources = [], showStats = true }: Props) => {
  const [selectedInstance, setSelectedInstance] = useState<CollectorInstanceView | null>(null);

  if (instances.length === 0) {
    return <EmptyState>No instances registered yet.</EmptyState>;
  }

  const online = instances.filter((i) => i.status === 'online').length;
  const offline = instances.length - online;
  const versions = new Set(instances.map((i) => i.version)).size;

  const getSourcesForInstance = (instance: CollectorInstanceView) =>
    sources.filter((s) => s.fleet_id === instance.fleet_id);

  return (
    <div>
      {showStats && (
        <StatsRow>
          <StatCard value={instances.length} label="Total" />
          <StatCard value={online} label="Online" variant="success" />
          <StatCard value={offline} label="Offline" variant="warning" />
          <StatCard value={versions} label="Versions" />
        </StatsRow>
      )}

      <Table striped highlightOnHover>
        <Table.Thead>
          <Table.Tr>
            <Table.Th>Status</Table.Th>
            <Table.Th>Hostname</Table.Th>
            <Table.Th>OS</Table.Th>
            <Table.Th>Fleet</Table.Th>
            <Table.Th>Last Seen</Table.Th>
            <Table.Th>Version</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {instances.map((instance) => (
            <ClickableRow key={instance.id} onClick={() => setSelectedInstance(instance)}>
              <Table.Td>
                <Badge color={instance.status === 'online' ? 'green' : 'gray'}>
                  {instance.status === 'online' ? 'Online' : 'Offline'}
                </Badge>
              </Table.Td>
              <Table.Td>{instance.hostname || instance.agent_id}</Table.Td>
              <Table.Td>
                <OsIcon os={instance.os} />
              </Table.Td>
              <Table.Td>
                <Link to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)} onClick={(e) => e.stopPropagation()}>
                  {fleetNames[instance.fleet_id] || instance.fleet_id}
                </Link>
              </Table.Td>
              <Table.Td>
                <RelativeTime dateTime={instance.last_seen} />
              </Table.Td>
              <Table.Td>{instance.version}</Table.Td>
            </ClickableRow>
          ))}
        </Table.Tbody>
      </Table>

      {selectedInstance && (
        <InstanceDetailDrawer
          instance={selectedInstance}
          sources={getSourcesForInstance(selectedInstance)}
          fleetName={fleetNames[selectedInstance.fleet_id] || selectedInstance.fleet_id}
          onClose={() => setSelectedInstance(null)}
        />
      )}
    </div>
  );
};

export default InstanceList;
