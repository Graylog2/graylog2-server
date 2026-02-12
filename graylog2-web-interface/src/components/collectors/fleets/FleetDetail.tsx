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
import styled, { css } from 'styled-components';

import {Button, Tab, Tabs, Label} from 'components/bootstrap';
import { Spinner } from 'components/common';
import PaginatedEntityTable from 'components/common/PaginatedEntityTable';
import type { SearchParams } from 'stores/PaginationTypes';

import FleetSettings from './FleetSettings';

import { useFleet, useFleetStats, useSources, fetchPaginatedSources, sourcesKeyFn, fetchPaginatedInstances, instancesKeyFn, useCollectorsMutations } from '../hooks';
import StatCard from '../common/StatCard';
import { InstanceDetailDrawer } from '../instances';
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

const StatsRow = styled.div(
  ({ theme }) => css`
    display: flex;
    margin-bottom: ${theme.spacings.lg};
    gap: ${theme.spacings.sm};
  `,
);

const FleetDetail = ({ fleetId }: Props) => {
  const { data: fleet, isLoading: fleetLoading } = useFleet(fleetId);
  const { data: stats, isLoading: statsLoading } = useFleetStats(fleetId);
  const { data: sources } = useSources(fleetId);
  const { createSource, isCreatingSource, updateSource, isUpdatingSource, updateFleet, isUpdatingFleet, deleteFleet, isDeletingFleet } = useCollectorsMutations();
  const [showSourceModal, setShowSourceModal] = useState(false);
  const [editingSource, setEditingSource] = useState<Source | null>(null);
  const [selectedInstance, setSelectedInstance] = useState<CollectorInstanceView | null>(null);

  const fleetNames = useMemo(() => (fleet ? { [fleet.id]: fleet.name } : {}), [fleet]);

  const instanceRenderers = useMemo(
    () => instanceColumnRenderers({ fleetNames }),
    [fleetNames],
  );

  const sourceRenderers = useMemo(() => sourceColumnRenderers(), []);

  const fetchInstances = useCallback(
    (searchParams: SearchParams) => fetchPaginatedInstances(searchParams),
    [fleetId],
  );

  const fetchSources = useCallback(
    (searchParams: SearchParams) => fetchPaginatedSources(searchParams, fleetId),
    [fleetId],
  );

  const instanceActions = useCallback(
    (instance: CollectorInstanceView) => (
      <Button bsStyle="link" bsSize="xs" onClick={() => setSelectedInstance(instance)}>
        Details
      </Button>
    ),
    [],
  );

  const sourceActions = useCallback(
    (source: Source) => (
      <Button bsStyle="link" bsSize="xs" onClick={() => setEditingSource(source)}>
        Edit
      </Button>
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
      await updateSource({ sourceId: editingSource.id, updates: source as Partial<Source> });
      setEditingSource(null);
    } else {
      await createSource(source);
      setShowSourceModal(false);
    }
  };

  return (
    <div>
      <Header>
        <h2>{fleet.name}</h2>
        {fleet.target_version && <Label bsStyle="info">v{fleet.target_version}</Label>}
      </Header>

      <StatsRow>
        <StatCard value={stats?.total_instances || 0} label="Instances" />
        <StatCard value={stats?.online_instances || 0} label="Online" variant="success" />
        <StatCard value={stats?.offline_instances || 0} label="Offline" variant="warning" />
        <StatCard value={stats?.total_sources || 0} label="Sources" />
      </StatsRow>

      <Tabs defaultActiveKey="sources" id="fleet-detail-tabs">
        <Tab eventKey="sources" title="Sources">
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1rem' }}>
            <Button bsStyle="success" onClick={() => setShowSourceModal(true)}>Add Source</Button>
          </div>
          <PaginatedEntityTable<Source>
            humanName="sources"
            tableLayout={SOURCES_LAYOUT}
            fetchEntities={fetchSources}
            keyFn={(params) => [...sourcesKeyFn(params), fleetId]}
            entityAttributesAreCamelCase={false}
            columnRenderers={sourceRenderers}
            entityActions={sourceActions}
          />
        </Tab>

        <Tab eventKey="instances" title="Instances">
          <PaginatedEntityTable<CollectorInstanceView>
            humanName="instances"
            entityActions={instanceActions}
            tableLayout={INSTANCES_LAYOUT}
            fetchEntities={fetchInstances}
            keyFn={(params) => [...instancesKeyFn(params), fleetId]}
            entityAttributesAreCamelCase={false}
            columnRenderers={instanceRenderers}
          />
        </Tab>

        <Tab eventKey="settings" title="Settings">
          <FleetSettings
            fleet={fleet}
            onSave={async (updates) => {
              await updateFleet({ fleetId: fleet.id, updates });
            }}
            onDelete={async () => {
              await deleteFleet(fleet.id);
              // Navigation back to fleets list will be handled by parent or router
            }}
            isLoading={isUpdatingFleet || isDeletingFleet}
          />
        </Tab>
      </Tabs>

      {showSourceModal && (
        <SourceFormModal
          fleetId={fleetId}
          onClose={() => setShowSourceModal(false)}
          onSave={handleSaveSource}
          isLoading={isCreatingSource}
        />
      )}

      {editingSource && (
        <SourceFormModal
          fleetId={fleetId}
          source={editingSource}
          onClose={() => setEditingSource(null)}
          onSave={handleSaveSource}
          isLoading={isUpdatingSource}
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
    </div>
  );
};

export default FleetDetail;
