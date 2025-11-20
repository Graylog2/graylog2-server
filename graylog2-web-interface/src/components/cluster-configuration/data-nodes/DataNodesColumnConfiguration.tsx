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
import NumberUtils from 'util/NumberUtils';
import Routes from 'routing/Routes';

import type { ClusterDataNode } from './useClusterDataNodes';

import { MetricsColumn, MetricsRow, RoleLabel, SecondaryText } from '../shared-components/NodeMetricsLayout';
import SizeAndRatioMetric from '../shared-components/SizeAndRatioMetric';

export const DEFAULT_VISIBLE_COLUMNS = ['hostname', 'datanode_status', 'memory', 'jvm', 'cpu', 'indexing', 'storage', 'datanode_version', 'opensearch_roles'] as const;

const JVM_WARNING_THRESHOLD = 0.7;
const JVM_DANGER_THRESHOLD = 0.9;
const MEMORY_WARNING_THRESHOLD = 0.7;
const MEMORY_DANGER_THRESHOLD = 0.9;
const STORAGE_WARNING_THRESHOLD = 0.7;
const STORAGE_DANGER_THRESHOLD = 0.9;

export const createColumnDefinitions = (): Array<ColumnSchema> => [
  { id: 'hostname', title: 'Node', sortable: true },
  { id: 'datanode_status', title: 'State', sortable: true },
  { id: 'memory', title: 'Memory', sortable: false, isDerived: true },
  { id: 'jvm', title: 'JVM', sortable: false, isDerived: true },
  { id: 'cpu', title: 'CPU', sortable: false, isDerived: true },
  { id: 'indexing', title: 'Indexing', sortable: false, isDerived: true },
  { id: 'storage', title: 'Storage', sortable: false, isDerived: true },
  { id: 'datanode_version', title: 'Version', sortable: true },
  { id: 'opensearch_roles', title: 'Role', sortable: true },
];

const getRoleLabels = (roles: Array<string>) =>
  roles.map((role) => (
    <span key={role}>
      <RoleLabel bsSize="xs">{role}</RoleLabel>&nbsp;
    </span>
  ));

const getDataNodeRoles = (dataNode: DataNode) =>
  dataNode.opensearch_roles?.map((currentRole) => currentRole.trim()).filter(Boolean) ?? [];

const formatNumberValue = (value: number | undefined | null, suffix = '') =>
  value == null ? '' : `${NumberUtils.formatNumber(value)}${suffix}`;

const calculateUsedFsBytes = (total: number | undefined | null, available: number | undefined | null) =>
  total != null && available != null ? total - available : undefined;

const formatIndexLatency = (total: number | undefined | null, timeInMillis: number | undefined | null) => {
  if (total == null || timeInMillis == null || timeInMillis === 0) {
    return '';
  }

  const latency = timeInMillis / total;

  return `${NumberUtils.formatNumber(latency)} ms/op`;
};

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
    },
    datanode_status: {
      renderCell: (_value, entity) => <DataNodeStatusCell dataNode={entity} />,
    },
    memory: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={entity.metrics?.usedMemory}
          max={entity.metrics?.totalMemory}
          warningThreshold={MEMORY_WARNING_THRESHOLD}
          dangerThreshold={MEMORY_DANGER_THRESHOLD}
        />
      ),
    },
    jvm: {
      renderCell: (_value, entity) => (
        <SizeAndRatioMetric
          used={entity.metrics?.jvmMemoryHeapUsed}
          max={entity.metrics?.jvmMemoryHeapMax}
          ratioPercent={entity.metrics?.opensearchJvmMemoryHeapUsedPercent}
          warningThreshold={JVM_WARNING_THRESHOLD}
          dangerThreshold={JVM_DANGER_THRESHOLD}
        />
      ),
    },
    cpu: {
      renderCell: (_value, entity) => {
        const loadAverage = entity.metrics?.cpuLoadAverage1m;
        const loadLabel = formatNumberValue(loadAverage);

        if (!loadLabel) {
          return null;
        }

        return (
          <MetricsColumn>
            <MetricsRow>
              <span>Load (1m)</span>
              <SecondaryText>
                <span>{loadLabel}</span>
              </SecondaryText>
            </MetricsRow>
          </MetricsColumn>
        );
      },
    },
    indexing: {
      renderCell: (_value, entity) => {
        const totalIndexed = entity.metrics?.indexTotal;
        const totalIndexedLabel = formatNumberValue(totalIndexed);
        const indexingLatencyLabel = formatIndexLatency(entity.metrics?.indexTotal, entity.metrics?.indexTimeInMillis);
        const hasContent = totalIndexedLabel || indexingLatencyLabel;

        if (!hasContent) {
          return null;
        }

        return (
          <MetricsColumn>
            {totalIndexedLabel && (
              <MetricsRow>
                <span>Docs</span>
                <SecondaryText>
                  <span>{totalIndexedLabel}</span>
                </SecondaryText>
              </MetricsRow>
            )}
            {indexingLatencyLabel && (
              <MetricsRow>
                <span>Latency</span>
                <SecondaryText>
                  <span>{indexingLatencyLabel}</span>
                </SecondaryText>
              </MetricsRow>
            )}
          </MetricsColumn>
        );
      },
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
    },
    datanode_version: {
      renderCell: (_value, entity) => (
        <SecondaryText>
          <span>{entity.datanode_version ?? 'N/A'}</span>
        </SecondaryText>
      ),
    },
    opensearch_roles: {
      renderCell: (_value, entity) => getRoleLabels(getDataNodeRoles(entity)),
    },
  },
});
