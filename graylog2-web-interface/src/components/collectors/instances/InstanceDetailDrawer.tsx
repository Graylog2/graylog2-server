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

import ActivityEntryList from '../common/ActivityEntryList';
import collectorReceivedMessagesUrl from '../common/collectorReceivedMessagesUrl';
import collectorSystemLogsUrl from '../common/collectorSystemLogsUrl';
import { useInstancePendingChanges } from '../hooks';
import type { ActivityEntry, CoalescedActions, CollectorInstanceView, Source } from '../types';

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

const CoalescedLabels = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    gap: ${theme.spacings.xs};
    margin-bottom: ${theme.spacings.sm};
  `,
);

// The net effect the collector will apply, as short summary chips. The reassign destination name
// comes resolved from the corresponding activity entry (the coalesced view only carries the id).
const coalescedSummary = (coalesced: CoalescedActions, activities: ActivityEntry[]): string[] => {
  const summary: string[] = [];

  if (coalesced.reassign_target_fleet_id) {
    const destination = activities
      .map((entry) => (entry.type === 'FLEET_REASSIGNED' ? entry.details?.destination_fleet?.name : null))
      .filter(Boolean)
      .pop();
    summary.push(destination ? `Reassign to ${destination}` : 'Reassign');
  }

  if (coalesced.recompute_config) summary.push('Configuration update');
  if (coalesced.recompute_ingest_config) summary.push('Ingest configuration update');
  if (coalesced.restart) summary.push('Restart');
  if (coalesced.run_discovery) summary.push('Discovery run');

  return summary;
};

const InstanceDetailDrawer = ({ instance, sources, fleetName, onClose }: Props) => {
  const osDescription = (instance.non_identifying_attributes?.['os.description'] as string) ?? null;
  const { data: pendingChanges } = useInstancePendingChanges(instance.instance_uid);
  const hasPendingChanges = (pendingChanges?.activities?.length ?? 0) > 0;

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
          <Title>Enrolled:</Title>
          <RelativeTime dateTime={instance.enrolled_at} />
        </DetailRow>

        <DetailRow>
          <Title>Version:</Title>
          <span>{instance.version || 'Unknown'}</span>
        </DetailRow>

        <DetailRow>
          <Title>Logs:</Title>
          <Link to={collectorSystemLogsUrl(instance.instance_uid)}>View System Logs</Link>
        </DetailRow>

        <DetailRow>
          <Title>Messages:</Title>
          <Link to={collectorReceivedMessagesUrl('collector_instance_uid', instance.instance_uid)}>
            Received messages
          </Link>
        </DetailRow>
      </Section>

      {hasPendingChanges && (
        <Section>
          <SectionTitle>Pending changes</SectionTitle>
          <CoalescedLabels>
            {coalescedSummary(pendingChanges.coalesced, pendingChanges.activities).map((summary) => (
              <Label key={summary} bsStyle="warning">
                {summary}
              </Label>
            ))}
          </CoalescedLabels>
          <EmptyText>Queued changes apply at the collector&apos;s next check-in.</EmptyText>
          <ActivityEntryList entries={pendingChanges.activities} />
        </Section>
      )}

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
          </tbody>
        </Table>
      </Section>

      <Section>
        <SectionTitle>Active Sources ({sources.length})</SectionTitle>
        <EmptyText>Sources are inherited from this instance&apos;s fleet configuration.</EmptyText>
        {sources.length === 0 ? (
          <EmptyText>
            No sources configured for this fleet. Add sources in the fleet detail page to start collecting data.
          </EmptyText>
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
