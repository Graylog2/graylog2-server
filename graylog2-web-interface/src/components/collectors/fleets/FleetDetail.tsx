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
import { useState, useMemo, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import styled, { css } from 'styled-components';
import URI from 'urijs';

import { Button, ButtonToolbar, DeleteMenuItem, SegmentedControl } from 'components/bootstrap';
import { ConfirmDialog, Link, Spinner } from 'components/common';
import BetaBadge from 'components/common/BetaBadge';
import { MoreActions } from 'components/common/EntityDataTable';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import useHistory from 'routing/useHistory';
import useQuery from 'routing/useQuery';
import Routes from 'routing/Routes';
import type { SearchParams } from 'stores/PaginationTypes';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import FleetSettings from './FleetSettings';

import {
  useFleet,
  useFleetStats,
  useSources,
  fetchPaginatedSources,
  sourcesKeyFn,
  fetchPaginatedInstances,
  instancesKeyFn,
  useCollectorsMutations,
  useDefaultInstanceFilters,
} from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import StatCard from '../common/StatCard';
import { InstanceDetailDrawer } from '../instances';
import BulkActions from '../instances/BulkActions';
import InstanceActions from '../instances/InstanceActions';
import instanceColumnRenderers from '../instances/ColumnRenderers';
import { DEFAULT_LAYOUT as INSTANCES_LAYOUT } from '../instances/Constants';
import sourceColumnRenderers from '../sources/ColumnRenderers';
import { DEFAULT_LAYOUT as SOURCES_LAYOUT } from '../sources/Constants';
import { SourceFormModal } from '../sources';
import type { CollectorInstanceView, Source } from '../types';

type Props = {
  fleetId: string;
};

const Header = styled.div(
  ({ theme }) => css`
    display: flex;
    margin-bottom: ${theme.spacings.lg};
    gap: ${theme.spacings.md};
    align-items: center;
  `,
);

const ActionsRow = styled.div(
  ({ theme }) => css`
    display: flex;
    justify-content: flex-end;
    margin-bottom: ${theme.spacings.md};
  `,
);

const StatsRow = styled.div(
  ({ theme }) => css`
    display: flex;
    margin-bottom: ${theme.spacings.lg};
    gap: ${theme.spacings.sm};
  `,
);

type FleetTab = 'sources' | 'instances' | 'settings';
const DEFAULT_TAB: FleetTab = 'sources';
const VALID_TABS: ReadonlyArray<FleetTab> = ['sources', 'instances', 'settings'];

const SEGMENTS = [
  { value: 'sources' as const, label: 'Sources' },
  { value: 'instances' as const, label: 'Instances' },
  { value: 'settings' as const, label: 'Settings' },
];

const FleetDetail = ({ fleetId }: Props) => {
  const queryClient = useQueryClient();
  const { data: fleet, isLoading: fleetLoading } = useFleet(fleetId);
  const { data: stats, isLoading: statsLoading } = useFleetStats(fleetId);
  const defaultInstanceFilters = useDefaultInstanceFilters();
  const { data: sources } = useSources(fleetId);
  const { createSource, updateSource, deleteSource, updateFleet, deleteFleet } = useCollectorsMutations();
  const [showSourceModal, setShowSourceModal] = useState(false);
  const [editingSource, setEditingSource] = useState<Source | null>(null);
  const [deletingSource, setDeletingSource] = useState<Source | null>(null);
  const [selectedInstance, setSelectedInstance] = useState<CollectorInstanceView | null>(null);

  const { tab: tabParam } = useQuery();
  const history = useHistory();
  const initialTab: FleetTab = VALID_TABS.includes(tabParam as FleetTab) ? (tabParam as FleetTab) : DEFAULT_TAB;
  const [activeTab, setActiveTab] = useState<FleetTab>(initialTab);

  const sendTelemetry = useSendCollectorsTelemetry();

  const navigateToTab = useCallback(
    (nextTab: FleetTab, filters?: Array<string>) => {
      setActiveTab(nextTab);

      let newUrl = new URI(window.location.href)
        .removeSearch('tab')
        .removeSearch('page')
        .removeSearch('pageSize')
        .removeSearch('query')
        .removeSearch('filters')
        .addSearch('tab', nextTab);

      if (filters?.length) {
        filters.forEach((f) => {
          newUrl = newUrl.addSearch('filters', f);
        });
      }

      history.replace(newUrl.resource());
    },
    [history],
  );

  const onTabChange = useCallback(
    (nextTab: FleetTab) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.FLEET.TAB_SELECTED, {
        app_action_value: `tab-${nextTab}`,
        fleet_id: fleetId,
        tab: nextTab,
      });
      navigateToTab(nextTab);
    },
    [fleetId, navigateToTab, sendTelemetry],
  );

  const fleetNames = useMemo(() => (fleet ? { [fleet.id]: fleet.name } : {}), [fleet]);

  const instanceRenderers = useMemo(() => instanceColumnRenderers({ fleetNames }), [fleetNames]);

  const sourceRenderers = useMemo(() => sourceColumnRenderers(), []);

  const fetchInstances = useCallback(
    (searchParams: SearchParams) => {
      const fleetQuery = `fleet_id:${fleetId}`;
      const query = searchParams.query ? `${fleetQuery} ${searchParams.query}` : fleetQuery;

      return fetchPaginatedInstances({ ...searchParams, query });
    },
    [fleetId],
  );

  const fetchSources = useCallback(
    (searchParams: SearchParams) => fetchPaginatedSources(searchParams, fleetId),
    [fleetId],
  );

  const instanceActions = useCallback(
    (instance: CollectorInstanceView) => <InstanceActions instance={instance} onDetailsClick={setSelectedInstance} />,
    [],
  );

  const handleConfirmDeleteSource = useCallback(async () => {
    if (!deletingSource) return;

    await deleteSource({ fleetId, sourceId: deletingSource.id });
    sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.SOURCE.DELETED, {
      app_action_value: 'source-delete',
      fleet_id: fleetId,
      source_id: deletingSource.id,
      source_type: deletingSource.type,
    });
    setDeletingSource(null);
  }, [deletingSource, deleteSource, fleetId, sendTelemetry]);

  const sourceActions = useCallback(
    (source: Source) => (
      <ButtonToolbar>
        <Button bsSize="xsmall" onClick={() => setEditingSource(source)}>
          Edit
        </Button>
        <MoreActions>
          <DeleteMenuItem onSelect={() => setDeletingSource(source)} />
        </MoreActions>
      </ButtonToolbar>
    ),
    [],
  );

  const getSourcesForInstance = (instance: CollectorInstanceView) =>
    (sources || []).filter((s) => s.fleet_id === instance.fleet_id);

  if (fleetLoading || statsLoading) {
    return <Spinner />;
  }

  if (!fleet) {
    return <div>Fleet not found</div>;
  }

  const handleSaveSource = async (source: Omit<Source, 'id'>) => {
    if (editingSource) {
      await updateSource({ fleetId, sourceId: editingSource.id, updates: source as Omit<Source, 'id' | 'fleet_id'> });
    } else {
      await createSource({ fleetId, source: source as Omit<Source, 'id' | 'fleet_id'> });
    }
  };

  return (
    <div>
      <Header>
        <h2>
          {fleet.name} <BetaBadge />
        </h2>
      </Header>

      <StatsRow>
        <StatCard
          value={stats?.total_instances ?? 0}
          label="Instances"
          helpText="Collector processes enrolled in this fleet."
          onClick={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.OVERVIEW.STAT_CARD_CLICKED, {
              app_action_value: 'stat-card-instances',
              card: 'instances',
              value: stats?.total_instances ?? 0,
              variant: 'default',
              navigates_to: 'instances',
              total_instances: stats?.total_instances ?? 0,
              online_instances: stats?.online_instances ?? 0,
              offline_instances: stats?.offline_instances ?? 0,
              total_fleets: 0,
              total_sources: stats?.total_sources ?? 0,
            });
            navigateToTab('instances');
          }}
        />
        <StatCard
          value={stats?.online_instances ?? 0}
          label="Online"
          helpText="Instances that reported a heartbeat within the offline threshold."
          variant="success"
          onClick={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.OVERVIEW.STAT_CARD_CLICKED, {
              app_action_value: 'stat-card-online',
              card: 'online',
              value: stats?.online_instances ?? 0,
              variant: 'success',
              navigates_to: 'instances-online',
              total_instances: stats?.total_instances ?? 0,
              online_instances: stats?.online_instances ?? 0,
              offline_instances: stats?.offline_instances ?? 0,
              total_fleets: 0,
              total_sources: stats?.total_sources ?? 0,
            });
            navigateToTab('instances', ['status=online']);
          }}
        />
        <StatCard
          value={stats?.offline_instances ?? 0}
          label="Offline"
          helpText="Instances that missed their heartbeat. Check host connectivity or collector process status."
          variant="warning"
          onClick={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.OVERVIEW.STAT_CARD_CLICKED, {
              app_action_value: 'stat-card-offline',
              card: 'offline',
              value: stats?.offline_instances ?? 0,
              variant: 'warning',
              navigates_to: 'instances-offline',
              total_instances: stats?.total_instances ?? 0,
              online_instances: stats?.online_instances ?? 0,
              offline_instances: stats?.offline_instances ?? 0,
              total_fleets: 0,
              total_sources: stats?.total_sources ?? 0,
            });
            navigateToTab('instances', ['status=offline']);
          }}
        />
        <StatCard
          value={stats?.total_sources ?? 0}
          label="Sources"
          helpText="Data collection configurations assigned to this fleet."
          onClick={() => {
            sendTelemetry(TELEMETRY_EVENT_TYPE.COLLECTORS.OVERVIEW.STAT_CARD_CLICKED, {
              app_action_value: 'stat-card-sources',
              card: 'sources',
              value: stats?.total_sources ?? 0,
              variant: 'default',
              navigates_to: 'sources',
              total_instances: stats?.total_instances ?? 0,
              online_instances: stats?.online_instances ?? 0,
              offline_instances: stats?.offline_instances ?? 0,
              total_fleets: 0,
              total_sources: stats?.total_sources ?? 0,
            });
            navigateToTab('sources');
          }}
        />
      </StatsRow>

      <SegmentedControl<FleetTab> data={SEGMENTS} value={activeTab} onChange={onTabChange} />

      {activeTab === 'sources' && (
        <>
          <p>
            Sources are automatically pushed to all collectors in this fleet. Changes take effect within seconds.
          </p>
          <ActionsRow>
            <Button bsStyle="primary" onClick={() => setShowSourceModal(true)}>
              Add Source
            </Button>
          </ActionsRow>
          <PaginatedEntityTable<Source>
            humanName="sources"
            tableLayout={SOURCES_LAYOUT}
            fetchEntities={fetchSources}
            keyFn={(params) => [...sourcesKeyFn(params), fleetId]}
            entityAttributesAreCamelCase={false}
            columnRenderers={sourceRenderers}
            entityActions={sourceActions}
          />
        </>
      )}

      {activeTab === 'instances' && (
        <>
        <p>
          Collector instances enrolled in this fleet. Each instance runs all enabled sources.
          To add more instances, <Link to={Routes.SYSTEM.COLLECTORS.DEPLOYMENT}>deploy collectors</Link> using
          an enrollment token for this fleet.
        </p>
        <PaginatedEntityTable<CollectorInstanceView>
          humanName="instances"
          entityActions={instanceActions}
          tableLayout={INSTANCES_LAYOUT}
          fetchEntities={fetchInstances}
          keyFn={(params) => [...instancesKeyFn(params), fleetId]}
          entityAttributesAreCamelCase={false}
          columnRenderers={instanceRenderers}
          defaultFilters={defaultInstanceFilters}
          bulkSelection={{ actions: <BulkActions /> }}
        />
        </>
      )}

      {activeTab === 'settings' && (
        <FleetSettings
          fleet={fleet}
          onSave={async (updates) => {
            await updateFleet({ fleetId: fleet.id, updates });
          }}
          onDelete={async () => {
            await deleteFleet(fleet.id);
            history.push(Routes.SYSTEM.COLLECTORS.FLEETS);
            // Invalidate after navigation so the fleets list refetches.
            // Fleet-specific queries were already removed by the mutation's onSuccess.
            queryClient.invalidateQueries({ queryKey: ['collectors'] });
          }}
        />
      )}

      {showSourceModal && (
        <SourceFormModal fleetId={fleetId} onClose={() => setShowSourceModal(false)} onSave={handleSaveSource} />
      )}

      {editingSource && (
        <SourceFormModal
          fleetId={fleetId}
          source={editingSource}
          onClose={() => setEditingSource(null)}
          onSave={handleSaveSource}
        />
      )}

      {selectedInstance && (
        <InstanceDetailDrawer
          instance={selectedInstance}
          sources={getSourcesForInstance(selectedInstance)}
          fleetName={fleetNames[selectedInstance.fleet_id] || selectedInstance.fleet_id}
          onClose={() => setSelectedInstance(null)}
        />
      )}

      {deletingSource && (
        <ConfirmDialog
          title="Delete source"
          show
          onConfirm={handleConfirmDeleteSource}
          onCancel={() => setDeletingSource(null)}>
          Are you sure you want to delete source <strong>{deletingSource.name}</strong>? Collectors in this fleet
          will stop collecting data from this source within seconds.
        </ConfirmDialog>
      )}
    </div>
  );
};

export default FleetDetail;
