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
import { useTranslation } from 'react-i18next';

import { Button, Label, Table } from 'components/bootstrap';
import Drawer from 'components/common/Drawer';
import { Icon, Link, RelativeTime, Spinner } from 'components/common';
import type { IconName } from 'components/common/Icon/types';
import { naturalSortIgnoreCase } from 'util/SortUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import InstanceHealthSection from './InstanceHealthSection';

import ActivityEntryList from '../common/ActivityEntryList';
import { DetailRow, DetailLabel } from '../common/DetailRow';
import FleetReference from '../common/FleetReference';
import { IconRow, IconRowList } from '../common/IconRowList';
import InstanceStatusLabel from '../common/InstanceStatusLabel';
import collectorOsName from '../common/collectorOsName';
import SyncStateIndicator from '../common/SyncStateIndicator';
import collectorReceivedMessagesUrl from '../common/collectorReceivedMessagesUrl';
import { AGENT_ID_FIELD } from '../common/fields';
import collectorSystemLogsUrl from '../common/collectorSystemLogsUrl';
import { useInstance, useInstancePendingChanges, useCollectorPermissions } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import { instanceTelemetryProps } from '../hooks/telemetry-helpers';
import type { CoalescedActions, CollectorInstanceView, Source, TargetInfo } from '../types';

type Props = {
  instance: CollectorInstanceView;
  sources: Source[];
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

const EmptyText = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
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

// `descriptionKey` is a translation key in the `collectors` namespace.
type PendingAction = { key: string; icon: IconName; descriptionKey: string };

// The Synchronization section's mutually-exclusive states, in precedence order.
type SyncStatus = 'error' | 'loading' | 'pending' | 'inSync';

// The net actions still awaiting the collector, as imperative one-liners.
const pendingActions = (coalesced: CoalescedActions): PendingAction[] => {
  const actions: PendingAction[] = [];

  if (coalesced.reassign) {
    actions.push({ key: 'reassign', icon: 'shuffle', descriptionKey: 'instance_drawer.action_reassign' });
  }

  if (coalesced.recompute_config) {
    actions.push({ key: 'config', icon: 'settings', descriptionKey: 'instance_drawer.action_reload_config' });
  }
  if (coalesced.recompute_ingest_config) {
    actions.push({
      key: 'ingest-config',
      icon: 'settings',
      descriptionKey: 'instance_drawer.action_reload_ingest_config',
    });
  }
  if (coalesced.restart) {
    actions.push({ key: 'restart', icon: 'refresh', descriptionKey: 'instance_drawer.action_restart' });
  }
  if (coalesced.run_discovery) {
    actions.push({ key: 'discovery', icon: 'search', descriptionKey: 'instance_drawer.action_discovery' });
  }

  return actions;
};

const InstanceDetailDrawer = ({ instance: instanceProp, sources, onClose }: Props) => {
  // The prop is a row snapshot frozen at drawer-open; poll the instance itself so
  // Status, Last Seen, and Health stay live (same pattern as the sync section below).
  // Errors here are non-fatal — we keep rendering the last known instance.
  const { t } = useTranslation('collectors');
  const { canReadSystemLogs } = useCollectorPermissions();
  const { data: freshInstance } = useInstance(instanceProp.instance_uid);
  const instance = freshInstance ?? instanceProp;
  const { data: pendingDetail, isError: pendingError } = useInstancePendingChanges(instance.instance_uid);
  // Use the backend's authoritative flag (consistent with the table); fall back to the table row's
  // value until the detail loads. Deriving from activities.length would wrongly show "In sync" for an
  // instance whose only pending markers are UNKNOWN (those are excluded from activities).
  const hasPendingChanges = pendingDetail ? pendingDetail.has_pending_changes : instance.has_pending_changes;
  const actions = pendingDetail ? pendingActions(pendingDetail.coalesced) : [];
  const activities = pendingDetail ? pendingDetail.activities : [];
  const [showTransactions, setShowTransactions] = useState(false);
  const sendTelemetry = useSendCollectorsTelemetry();

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
          <DetailLabel>{t('instance_drawer.status')}</DetailLabel>
          <InstanceStatusLabel status={instance.status} />
        </DetailRow>

        <DetailRow>
          <DetailLabel>{t('instance_drawer.sync')}</DetailLabel>
          <SyncStateIndicator pending={hasPendingChanges} withLabel />
        </DetailRow>

        <DetailRow>
          <DetailLabel>{t('instance_drawer.fleet')}</DetailLabel>
          <FleetReference
            fleetId={instance.fleet_id}
            pendingFleetId={instance.pending_fleet_id}
            onClick={() =>
              sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.FLEET_OPENED, {
                app_action_value: 'instance-drawer-open-fleet',
                ...instanceTelemetryProps(instance),
              })
            }
          />
        </DetailRow>

        <DetailRow>
          <DetailLabel>{t('instance_drawer.os')}</DetailLabel>
          <span>{collectorOsName(instance, true)}</span>
        </DetailRow>

        <DetailRow>
          <DetailLabel>{t('instance_drawer.last_seen')}</DetailLabel>
          <RelativeTime dateTime={instance.last_seen} />
        </DetailRow>

        <DetailRow>
          <DetailLabel>{t('instance_drawer.enrolled')}</DetailLabel>
          <RelativeTime dateTime={instance.enrolled_at} />
        </DetailRow>

        <DetailRow>
          <DetailLabel>{t('instance_drawer.version')}</DetailLabel>
          <span>{instance.version || t('os.unknown')}</span>
        </DetailRow>

        {canReadSystemLogs && (
          <DetailRow>
            <DetailLabel>{t('instance_drawer.logs')}</DetailLabel>
            <Link
              to={collectorSystemLogsUrl(instance.instance_uid)}
              onClick={() =>
                sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.VIEW_LOGS_CLICKED, {
                  app_action_value: 'instance-drawer-view-logs',
                  ...instanceTelemetryProps(instance),
                  origin: 'detail-drawer',
                })
              }>
              {t('instance_actions.view_system_logs')}
            </Link>
          </DetailRow>
        )}

        <DetailRow>
          <DetailLabel>{t('instance_drawer.messages')}</DetailLabel>
          <Link
            to={collectorReceivedMessagesUrl(AGENT_ID_FIELD, instance.instance_uid)}
            onClick={() =>
              sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.RECEIVED_MESSAGES_CLICKED, {
                app_action_value: 'instance-drawer-received-messages',
                ...instanceTelemetryProps(instance),
                origin: 'detail-drawer',
              })
            }>
            {t('fleet_actions.received_messages')}
          </Link>
        </DetailRow>
      </Section>

      <InstanceHealthSection health={instance.health} online={instance.status === 'online'} />

      <Section>
        <SectionTitle>{t('instance_drawer.attributes')}</SectionTitle>
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
        <SectionTitle>{t('instance_drawer.active_sources', { count: sources.length })}</SectionTitle>
        <EmptyText>{t('instance_drawer.sources_inherited')}</EmptyText>
        {sources.length === 0 ? (
          <EmptyText>{t('instance_drawer.no_sources')}</EmptyText>
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
        <SectionTitle>{t('instance_drawer.synchronization')}</SectionTitle>
        {syncStatus === 'error' && <EmptyText>{t('instance_drawer.pending_load_error')}</EmptyText>}
        {/* Spin until the detail loads regardless of the table-row flag, which can be stale and would
            otherwise flash a contradictory "In sync" before the fetched state arrives. */}
        {syncStatus === 'loading' && <Spinner />}
        {syncStatus === 'pending' &&
          (hasDescribableChanges ? (
            <>
              <span>{t('instance_drawer.queued_actions')}</span>
              <ActionList>
                {actions.map((action) => (
                  <IconRow key={action.key}>
                    <Icon name={action.icon} />
                    <span>{t(action.descriptionKey)}</span>
                  </IconRow>
                ))}
              </ActionList>
              <TransactionsToggle
                bsStyle="link"
                bsSize="xsmall"
                onClick={() => {
                  sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.INSTANCE.TRANSACTIONS_TOGGLED, {
                    app_action_value: 'instance-drawer-toggle-transactions',
                    ...instanceTelemetryProps(instance),
                    // The state being switched to, and how much is queued behind it.
                    shown: !showTransactions,
                    queued_count: activities.length,
                  });

                  setShowTransactions((show) => !show);
                }}>
                {showTransactions
                  ? t('instance_drawer.hide_transactions')
                  : t('instance_drawer.show_transactions', { count: activities.length })}
              </TransactionsToggle>
              {showTransactions && <ActivityEntryList entries={activities} compareTargets={compareTargets} />}
            </>
          ) : (
            // Pending, but every queued marker is of a type this version can't describe (UNKNOWN).
            <EmptyText>{t('instance_drawer.changes_queued')}</EmptyText>
          ))}
        {syncStatus === 'inSync' && (
          <>
            <SyncStateIndicator pending={false} withLabel />
            <br />
            <EmptyText>{t('instance_drawer.all_applied')}</EmptyText>
          </>
        )}
      </Section>
    </Drawer>
  );
};

export default InstanceDetailDrawer;
