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
import type { ColorVariant } from '@graylog/sawmill';

import { Badge, Table } from 'components/bootstrap';
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

const DetailRow = styled.div`
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
`;

const Label = styled.span`
  font-weight: 500;
  min-width: 120px;
  font-size: 0.875rem;
`;

const SourceList = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacings.xs};
`;

const configStatusStyles: Record<string, ColorVariant> = {
  APPLIED: 'success',
  APPLYING: 'info',
  FAILED: 'danger',
  UNSET: 'default',
};

const InstanceDetailDrawer = ({ instance, sources, fleetName, onClose }: Props) => {
  const osDescription = instance.agent_description.non_identifying_attributes.find(
    (attr) => attr.key === 'os.description',
  )?.value;

  return (
    <Drawer title={instance.hostname || instance.agent_id} onClose={onClose} size="md">
      <Section>
        <DetailRow>
          <Label>Status:</Label>
          <Badge bsStyle={instance.status === 'online' ? 'success' : 'default'}>
            {instance.status === 'online' ? 'Online' : 'Offline'}
          </Badge>
        </DetailRow>

        <DetailRow>
          <Label>Fleet:</Label>
          <Link to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)}>{fleetName}</Link>
        </DetailRow>

        <DetailRow>
          <Label>OS:</Label>
          <span>{osDescription || instance.os || 'Unknown'}</span>
        </DetailRow>

        <DetailRow>
          <Label>Last Seen:</Label>
          <RelativeTime dateTime={instance.last_seen} />
        </DetailRow>

        <DetailRow>
          <Label>Version:</Label>
          <span>{instance.version}</span>
        </DetailRow>

        <DetailRow>
          <Label>Config:</Label>
          <span style={{ display: 'inline-flex', gap: '0.25rem', alignItems: 'center' }}>
            <code>{instance.remote_config_status.last_remote_config_hash.slice(0, 7)}</code>
            <Badge bsStyle={configStatusStyles[instance.remote_config_status.status]}>
              {instance.remote_config_status.status}
            </Badge>
          </span>
        </DetailRow>

        {instance.remote_config_status.error_message && (
          <DetailRow>
            <Label>Error:</Label>
            <span style={{ color: 'red' }}>{instance.remote_config_status.error_message}</span>
          </DetailRow>
        )}
      </Section>

      <Section>
        <SectionTitle>System Details</SectionTitle>
        <Table striped>
          <tbody>
            {instance.agent_description.identifying_attributes.map((attr) => (
              <tr key={attr.key}>
                <td>{attr.key}</td>
                <td>{attr.value}</td>
              </tr>
            ))}
            {instance.agent_description.non_identifying_attributes.map((attr) => (
              <tr key={attr.key}>
                <td>{attr.key}</td>
                <td>{attr.value}</td>
              </tr>
            ))}
            <tr>
              <td>connection_type</td>
              <td>{instance.connection_type}</td>
            </tr>
            <tr>
              <td>first_seen</td>
              <td><RelativeTime dateTime={instance.first_seen} /></td>
            </tr>
          </tbody>
        </Table>
      </Section>

      <Section>
        <SectionTitle>Active Sources ({sources.length})</SectionTitle>
        {sources.length === 0 ? (
          <span style={{ color: '#666' }}>No sources configured for this fleet.</span>
        ) : (
          <SourceList>
            {sources.map((source) => (
              <span key={source.id} style={{ display: 'inline-flex', gap: '0.25rem', alignItems: 'center' }}>
                <span>• {source.name}</span>
                <Badge bsStyle="info">{source.type}</Badge>
              </span>
            ))}
          </SourceList>
        )}
      </Section>
    </Drawer>
  );
};

export default InstanceDetailDrawer;
