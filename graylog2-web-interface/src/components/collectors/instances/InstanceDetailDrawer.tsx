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
import { Badge, Table, Stack, Text, Group } from '@mantine/core';

import Drawer from 'components/common/Drawer';
import { RelativeTime } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

import type { CollectorInstanceView, Source } from '../types';

type Props = {
  instance: CollectorInstanceView;
  sources: Source[];
  fleetName: string;
  onClose: () => void;
};

const Section = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.md};
  `,
);

const SectionTitle = styled.h4(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
    font-size: ${theme.fonts.size.body};
    font-weight: 600;
    border-bottom: 1px solid ${theme.colors.gray[80]};
    padding-bottom: ${theme.spacings.xs};
  `,
);

const DetailRow = styled(Group)`
  margin-bottom: 0.5rem;
`;

const Label = styled(Text)`
  font-weight: 500;
  min-width: 120px;
`;

const configStatusColors: Record<string, string> = {
  APPLIED: 'green',
  APPLYING: 'blue',
  FAILED: 'red',
  UNSET: 'gray',
};

const InstanceDetailDrawer = ({ instance, sources, fleetName, onClose }: Props) => {
  const osDescription = instance.agent_description.non_identifying_attributes.find(
    (attr) => attr.key === 'os.description',
  )?.value;

  return (
    <Drawer title={instance.hostname || instance.agent_id} onClose={onClose} size="md">
      <Stack gap="md">
        <Section>
          <DetailRow>
            <Label size="sm">Status:</Label>
            <Badge color={instance.status === 'online' ? 'green' : 'gray'}>
              {instance.status === 'online' ? 'Online' : 'Offline'}
            </Badge>
          </DetailRow>

          <DetailRow>
            <Label size="sm">Fleet:</Label>
            <Link to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)}>{fleetName}</Link>
          </DetailRow>

          <DetailRow>
            <Label size="sm">OS:</Label>
            <Text size="sm">{osDescription || instance.os || 'Unknown'}</Text>
          </DetailRow>

          <DetailRow>
            <Label size="sm">Last Seen:</Label>
            <RelativeTime dateTime={instance.last_seen} />
          </DetailRow>

          <DetailRow>
            <Label size="sm">Version:</Label>
            <Text size="sm">{instance.version}</Text>
          </DetailRow>

          <DetailRow>
            <Label size="sm">Config:</Label>
            <Group gap="xs">
              <Text size="sm" ff="monospace">{instance.remote_config_status.last_remote_config_hash.slice(0, 7)}</Text>
              <Badge color={configStatusColors[instance.remote_config_status.status]} size="sm">
                {instance.remote_config_status.status}
              </Badge>
            </Group>
          </DetailRow>

          {instance.remote_config_status.error_message && (
            <DetailRow>
              <Label size="sm">Error:</Label>
              <Text size="sm" c="red">{instance.remote_config_status.error_message}</Text>
            </DetailRow>
          )}
        </Section>

        <Section>
          <SectionTitle>System Details</SectionTitle>
          <Table striped>
            <Table.Tbody>
              {instance.agent_description.identifying_attributes.map((attr) => (
                <Table.Tr key={attr.key}>
                  <Table.Td>{attr.key}</Table.Td>
                  <Table.Td>{attr.value}</Table.Td>
                </Table.Tr>
              ))}
              {instance.agent_description.non_identifying_attributes.map((attr) => (
                <Table.Tr key={attr.key}>
                  <Table.Td>{attr.key}</Table.Td>
                  <Table.Td>{attr.value}</Table.Td>
                </Table.Tr>
              ))}
              <Table.Tr>
                <Table.Td>connection_type</Table.Td>
                <Table.Td>{instance.connection_type}</Table.Td>
              </Table.Tr>
              <Table.Tr>
                <Table.Td>first_seen</Table.Td>
                <Table.Td><RelativeTime dateTime={instance.first_seen} /></Table.Td>
              </Table.Tr>
            </Table.Tbody>
          </Table>
        </Section>

        <Section>
          <SectionTitle>Active Sources ({sources.length})</SectionTitle>
          {sources.length === 0 ? (
            <Text size="sm" c="dimmed">No sources configured for this fleet.</Text>
          ) : (
            <Stack gap="xs">
              {sources.map((source) => (
                <Group key={source.id} gap="xs">
                  <Text size="sm">• {source.name}</Text>
                  <Badge size="xs" variant="light">{source.type}</Badge>
                </Group>
              ))}
            </Stack>
          )}
        </Section>
      </Stack>
    </Drawer>
  );
};

export default InstanceDetailDrawer;
