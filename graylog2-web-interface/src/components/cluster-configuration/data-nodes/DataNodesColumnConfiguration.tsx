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

import type { ColumnRenderers, ColumnSchema } from 'components/common/EntityDataTable';
import { Link } from 'components/common/router';
import DataNodeStatusCell from 'components/datanode/DataNodeList/DataNodeStatusCell';
import type { DataNode } from 'components/datanode/Types';
import Routes from 'routing/Routes';

import type { ClusterDataNode } from './fetchClusterDataNodes';
import IndexingMetricsCell from './cells/IndexingMetricsCell';

import CpuMetricsCell from '../shared-components/CpuMetricsCell';
import { RoleLabel, SecondaryText } from '../shared-components/NodeMetricsLayout';
import SizeAndRatioMetric from '../shared-components/SizeAndRatioMetric';

export const DEFAULT_VISIBLE_COLUMNS = [
  'hostname',
  'opensearch_roles',
  'datanode_version',
  'datanode_status',
  'cpu',
  'memory',
  'jvm',
  'indexing',
  'storage',
];

const JVM_WARNING_THRESHOLD = 0.95;
const MEMORY_WARNING_THRESHOLD = 0.95;
const STORAGE_WARNING_THRESHOLD = 0.7;
const STORAGE_DANGER_THRESHOLD = 0.8;
const CPU_WARNING_THRESHOLD = 0.7;
const CPU_DANGER_THRESHOLD = 0.9;

export const createColumnDefinitions = (): Array<ColumnSchema> => [
  { id: 'cpu', title: 'CPU', sortable: false, isDerived: true },
  { id: 'memory', title: 'Memory', sortable: false, isDerived: true },
  { id: 'jvm', title: 'JVM', sortable: false, isDerived: true },
  { id: 'indexing', title: 'Indexing', sortable: false, isDerived: true },
  { id: 'storage', title: 'Storage', sortable: false, isDerived: true },
];

const getRoleLabels = (roles: Array<string>) =>
  roles.map((role) => (
    <span key={role}>
      <RoleLabel bsSize="xs">{role}</RoleLabel>&nbsp;
    </span>
  ));

const getDataNodeRoles = (dataNode: DataNode) =>
  dataNode.opensearch_roles?.map((currentRole) => currentRole.trim()).filter(Boolean) ?? [];

const calculateUsedFsBytes = (total: number | undefined | null, available: number | undefined | null) =>
  total != null && available != null ? total - available : undefined;

export const createColumnRenderers = (): ColumnRenderers<ClusterDataNode> => ({
  attributes: {
    hostname: {
      renderCell: (_value, entity) => {
        const datanodeRouteId = entity.node_id ?? entity.id;
        const nodeName = entity.hostname ?? datanodeRouteId;

        if (!datanodeRouteId) {
          return nodeName;
        }

        return <Link to={Routes.SYSTEM.CLUSTER.DATANODE_SHOW(datanodeRouteId)}>{nodeName}</Link>;
      },
      minWidth: 300,
    },
    datanode_status: {
      renderCell: (_value, entity) => <DataNodeStatusCell dataNode={entity} />,
      staticWidth: 130,
    },
    memory: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={entity.metrics?.usedMemory}
          max={entity.metrics?.totalMemory}
          warningThreshold={MEMORY_WARNING_THRESHOLD}
        />
      ),
      staticWidth: 130,
    },
    jvm: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={entity.metrics?.jvmMemoryHeapUsed}
          max={entity.metrics?.jvmMemoryHeapMax}
          ratioPercent={entity.metrics?.jvmMemoryHeapUsedPercent}
          warningThreshold={JVM_WARNING_THRESHOLD}
        />
      ),
      staticWidth: 130,
    },
    cpu: {
      renderCell: (_value, entity) => (
        <CpuMetricsCell
          loadAverage={entity.metrics?.cpuLoadAverage1m}
          cpuPercent={entity.metrics?.cpuPercent}
          warningThreshold={CPU_WARNING_THRESHOLD}
          dangerThreshold={CPU_DANGER_THRESHOLD}
        />
      ),
      staticWidth: 130,
    },
    indexing: {
      renderCell: (_value, entity) => (
        <IndexingMetricsCell
          totalIndexed={entity.metrics?.indexTotal}
          indexTimeInMillis={entity.metrics?.indexTimeInMillis}
        />
      ),
      staticWidth: 130,
    },
    storage: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={calculateUsedFsBytes(entity.metrics?.totalFsBytes, entity.metrics?.availableFsBytes)}
          max={entity.metrics?.totalFsBytes}
          warningThreshold={STORAGE_WARNING_THRESHOLD}
          dangerThreshold={STORAGE_DANGER_THRESHOLD}
        />
      ),
      staticWidth: 130,
    },
    datanode_version: {
      renderCell: (_value, entity) => (
        <SecondaryText>
          <span>{entity.datanode_version ?? 'N/A'}</span>
        </SecondaryText>
      ),
      minWidth: 200,
    },
    opensearch_roles: {
      renderCell: (_value, entity) => getRoleLabels(getDataNodeRoles(entity)),
      minWidth: 220,
    },
  },
});
