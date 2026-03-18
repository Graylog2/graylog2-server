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
import { Label, Table } from 'components/bootstrap';
import Drawer from 'components/common/Drawer';
import { Link, RelativeTime } from 'components/common';
import Routes from 'routing/Routes';

import collectorLogsUrl from '../common/collectorLogsUrl';
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

const DetailRow = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.xs};
    margin-bottom: ${theme.spacings.xs};
  `,
);

const Title = styled.span(
  ({ theme }) => css`
    font-weight: 500;
    min-width: 120px;
    font-size: ${theme.fonts.size.small};
  `,
);

const EmptyText = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
  `,
);

const SourceItem = styled.span(
  ({ theme }) => css`
    display: inline-flex;
    gap: ${theme.spacings.xxs};
    align-items: center;
  `,
);

const SourceList = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacings.xs};
`;

const InstanceDetailDrawer = ({ instance, sources, fleetName, onClose }: Props) => {
  const osDescription = (instance.non_identifying_attributes?.['os.description'] as string) ?? null;

  return (
    <Drawer title={instance.hostname || instance.instance_uid} onClose={onClose} size="md">
      <Section>
        <DetailRow>
          <Title>Status:</Title>
          <Label bsStyle={instance.status === 'online' ? 'success' : 'default'}>
            {instance.status === 'online' ? 'Online' : 'Offline'}
          </Label>
        </DetailRow>

        <DetailRow>
          <Title>Fleet:</Title>
          <Link to={Routes.SYSTEM.COLLECTORS.FLEET(instance.fleet_id)}>{fleetName}</Link>
        </DetailRow>

        <DetailRow>
          <Title>OS:</Title>
          <span>{osDescription || instance.os || 'Unknown'}</span>
        </DetailRow>

        <DetailRow>
          <Title>Last Seen:</Title>
          <RelativeTime dateTime={instance.last_seen} />
        </DetailRow>

        <DetailRow>
          <Title>Version:</Title>
          <span>{instance.version || 'Unknown'}</span>
        </DetailRow>

        <DetailRow>
          <Title>Logs:</Title>
          <Link to={collectorLogsUrl(instance.instance_uid)}>View Logs</Link>
        </DetailRow>
      </Section>

      <Section>
        <SectionTitle>Attributes</SectionTitle>
        <Table striped>
          <tbody>
            {Object.entries(instance.identifying_attributes).map(([key, value]) => (
              <tr key={key}>
                <td>{key}</td>
                <td>{String(value)}</td>
              </tr>
            ))}
            {Object.entries(instance.non_identifying_attributes).map(([key, value]) => (
              <tr key={key}>
                <td>{key}</td>
                <td>{String(value)}</td>
              </tr>
            ))}
            <tr>
              <td>enrolled_at</td>
              <td><RelativeTime dateTime={instance.enrolled_at} /></td>
            </tr>
          </tbody>
        </Table>
      </Section>

      <Section>
        <SectionTitle>Active Sources ({sources.length})</SectionTitle>
        {sources.length === 0 ? (
          <EmptyText>No sources configured for this fleet.</EmptyText>
        ) : (
          <SourceList>
            {sources.map((source) => (
              <SourceItem key={source.id}>
                <span>• {source.name}</span>
                <Label bsStyle="info">{source.type}</Label>
              </SourceItem>
            ))}
          </SourceList>
        )}
      </Section>
    </Drawer>
  );
};

export default InstanceDetailDrawer;
