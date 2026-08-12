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

import { Button, Label, Table } from 'components/bootstrap';
import Drawer from 'components/common/Drawer';
import { Icon, Link, RelativeTime, Spinner } from 'components/common';
import type { IconName } from 'components/common/Icon/types';
import Routes from 'routing/Routes';
import { naturalSortIgnoreCase } from 'util/SortUtils';

import ActivityEntryList from '../common/ActivityEntryList';
import { IconRow, IconRowList } from '../common/IconRowList';
import SyncStateIndicator from '../common/SyncStateIndicator';
import collectorReceivedMessagesUrl from '../common/collectorReceivedMessagesUrl';
import { COLLECTOR_INSTANCE_UID_FIELD } from '../common/fields';
import collectorSystemLogsUrl from '../common/collectorSystemLogsUrl';
import { useInstancePendingChanges } from '../hooks';
import type { CoalescedActions, CollectorInstanceView, Source, TargetInfo } from '../types';

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

const ActionList = styled(IconRowList)(
  ({ theme }) => css`
    margin-top: ${theme.spacings.xs};
  `,
);

const TransactionsToggle = styled(Button)`
  padding-left: 0;
`;

type PendingAction = { key: string; icon: IconName; description: React.ReactNode };

// The Synchronization section's mutually-exclusive states, in precedence order.
type SyncStatus = 'error' | 'loading' | 'pending' | 'inSync';

// The net actions still awaiting the collector, as imperative one-liners.
const pendingActions = (coalesced: CoalescedActions): PendingAction[] => {
  const actions: PendingAction[] = [];

  if (coalesced.reassign) {
    actions.push({ key: 'reassign', icon: 'shuffle', description: 'Reassign to another fleet' });
  }

  if (coalesced.recompute_config) {
    actions.push({ key: 'config', icon: 'settings', description: 'Reload configuration' });
  }
  if (coalesced.recompute_ingest_config) {
    actions.push({ key: 'ingest-config', icon: 'settings', description: 'Reload ingest configuration' });
  }
  if (coalesced.restart) {
    actions.push({ key: 'restart', icon: 'refresh', description: 'Restart' });
  }
  if (coalesced.run_discovery) {
    actions.push({ key: 'discovery', icon: 'search', description: 'Run source discovery' });
  }

  return actions;
};

const InstanceDetailDrawer = ({ instance, sources, fleetName, onClose }: Props) => {
  const osDescription = (instance.non_identifying_attributes?.['os.description'] as string) ?? null;
  const { data: pendingDetail, isError: pendingError } = useInstancePendingChanges(instance.instance_uid);
  // Use the backend's authoritative flag (consistent with the table); fall back to the table row's
  // value until the detail loads. Deriving from activities.length would wrongly show "In sync" for an
  // instance whose only pending markers are UNKNOWN (those are excluded from activities).
  const hasPendingChanges = pendingDetail ? pendingDetail.has_pending_changes : instance.has_pending_changes;
  const actions = pendingDetail ? pendingActions(pendingDetail.coalesced) : [];
  const activities = pendingDetail ? pendingDetail.activities : [];
  const [showTransactions, setShowTransactions] = useState(false);

  let syncStatus: SyncStatus;
  if (pendingDetail) {
    // Have data: show it even if a refetch errored, so a transient blip can't wipe a valid list.
    syncStatus = hasPendingChanges ? 'pending' : 'inSync';
  } else if (pendingError) {
    // No data and the load failed; the failure is also surfaced via the query hook's toast.
    syncStatus = 'error';
  } else {
    syncStatus = 'loading';
  }
  // Among pending instances, whether any queued change is describable (vs only UNKNOWN markers).
  const hasDescribableChanges = actions.length > 0 || activities.length > 0;

  // In this per-instance view a bulk reassignment lists every collector in the batch. Float the
  // collector we're viewing to the front so it leads the description instead of an arbitrary one.
  // Only collector targets are matched against the instance uid; fleet targets just sort by name.
  const compareTargets = (a: TargetInfo, b: TargetInfo) => {
    if (a.type === 'collector' && a.id === instance.instance_uid) return -1;
    if (b.type === 'collector' && b.id === instance.instance_uid) return 1;

    return naturalSortIgnoreCase(a.name, b.name);
  };

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
          <Title>Sync:</Title>
          <SyncStateIndicator pending={hasPendingChanges} withLabel />
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
          <Link to={collectorReceivedMessagesUrl(COLLECTOR_INSTANCE_UID_FIELD, instance.instance_uid)}>
            Received messages
          </Link>
        </DetailRow>
      </Section>

      <Section>
        <SectionTitle>Attributes</SectionTitle>
        <Table>
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

      <Section>
        <SectionTitle>Synchronization</SectionTitle>
        {syncStatus === 'error' && <EmptyText>Could not load pending changes. Please try again later.</EmptyText>}
        {/* Spin until the detail loads regardless of the table-row flag, which can be stale and would
            otherwise flash a contradictory "In sync" before the fetched state arrives. */}
        {syncStatus === 'loading' && <Spinner />}
        {syncStatus === 'pending' &&
          (hasDescribableChanges ? (
            <>
              <span>The following actions are queued until the collector synchronizes:</span>
              <ActionList>
                {actions.map((action) => (
                  <IconRow key={action.key}>
                    <Icon name={action.icon} />
                    <span>{action.description}</span>
                  </IconRow>
                ))}
              </ActionList>
              <TransactionsToggle bsStyle="link" bsSize="xsmall" onClick={() => setShowTransactions((show) => !show)}>
                {showTransactions ? 'Hide queued transactions' : `Show queued transactions (${activities.length})`}
              </TransactionsToggle>
              {showTransactions && <ActivityEntryList entries={activities} compareTargets={compareTargets} />}
            </>
          ) : (
            // Pending, but every queued marker is of a type this version can't describe (UNKNOWN).
            <EmptyText>Changes are queued and will be applied at the collector&apos;s next check-in.</EmptyText>
          ))}
        {syncStatus === 'inSync' && (
          <>
            <SyncStateIndicator pending={false} withLabel />
            <br />
            <EmptyText>The collector has applied all queued actions.</EmptyText>
          </>
        )}
      </Section>
    </Drawer>
  );
};

export default InstanceDetailDrawer;
