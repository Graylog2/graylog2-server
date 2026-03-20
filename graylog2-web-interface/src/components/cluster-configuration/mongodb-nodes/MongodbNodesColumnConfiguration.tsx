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
import React from 'react';

import type { ColumnRenderers } from 'components/common/EntityDataTable';

import type { MongodbNode } from './fetchClusterMongodbNodes';
import CountCell from './cells/CountCell';
import PercentRatioCell from './cells/PercentRatioCell';
import ProfilingLevelCell from './cells/ProfilingLevelCell';
import ReplicationLagCell from './cells/ReplicationLagCell';

import { RoleLabel, SecondaryText } from '../shared-components/NodeMetricsLayout';

const REPLICATION_LAG_WARNING_THRESHOLD_MS = 1000;
const REPLICATION_LAG_DANGER_THRESHOLD_MS = 30000;
const STORAGE_WARNING_THRESHOLD = 0.7;
const STORAGE_DANGER_THRESHOLD = 0.8;
const CONNECTIONS_WARNING_THRESHOLD = 0.7;
const CONNECTIONS_DANGER_THRESHOLD = 0.9;

export const DEFAULT_VISIBLE_COLUMNS = [
  'name',
  'role',
  'version',
  'replication_lag',
  'profiling_level',
  'slow_query_count',
  'storage_used_percent',
  'connections_used_percent',
];

export const createColumnRenderers = (): ColumnRenderers<MongodbNode> => ({
  attributes: {
    name: {
      renderCell: (_value, entity) => entity.name ?? 'N/A',
      minWidth: 250,
    },
    role: {
      renderCell: (_value, entity) => {
        const role = entity.role?.toUpperCase();

        if (!role) {
          return (
            <SecondaryText>
              <span>N/A</span>
            </SecondaryText>
          );
        }

        return <RoleLabel bsSize="xs">{role}</RoleLabel>;
      },
      minWidth: 130,
    },
    version: {
      renderCell: (_value, entity) => (
        <SecondaryText>
          <span>{entity.version ?? 'N/A'}</span>
        </SecondaryText>
      ),
      minWidth: 120,
    },
    replication_lag: {
      renderCell: (_value, entity) => (
        <ReplicationLagCell
          replicationLag={entity.replication_lag}
          role={entity.role}
          warningThreshold={REPLICATION_LAG_WARNING_THRESHOLD_MS}
          dangerThreshold={REPLICATION_LAG_DANGER_THRESHOLD_MS}
        />
      ),
      staticWidth: 'matchHeader',
    },
    profiling_level: {
      renderCell: (_value, entity) => <ProfilingLevelCell profilingLevel={entity.profiling_level} />,
      staticWidth: 'matchHeader',
    },
    slow_query_count: {
      renderCell: (_value, entity) => <CountCell count={entity.slow_query_count} />,
      staticWidth: 'matchHeader',
    },
    storage_used_percent: {
      renderCell: (_value, entity) => (
        <PercentRatioCell
          percent={entity.storage_used_percent}
          warningThreshold={STORAGE_WARNING_THRESHOLD}
          dangerThreshold={STORAGE_DANGER_THRESHOLD}
        />
      ),
      staticWidth: 'matchHeader',
    },
    available_connections: {
      renderCell: (_value, entity) => <CountCell count={entity.available_connections} />,
      staticWidth: 'matchHeader',
    },
    current_connections: {
      renderCell: (_value, entity) => <CountCell count={entity.current_connections} />,
      staticWidth: 'matchHeader',
    },
    connections_used_percent: {
      renderCell: (_value, entity) => (
        <PercentRatioCell
          percent={entity.connections_used_percent}
          warningThreshold={CONNECTIONS_WARNING_THRESHOLD}
          dangerThreshold={CONNECTIONS_DANGER_THRESHOLD}
        />
      ),
      staticWidth: 'matchHeader',
    },
  },
});
