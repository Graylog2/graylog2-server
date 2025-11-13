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
import styled from 'styled-components';

import { Label } from 'components/bootstrap';
import type { ColumnRenderers, ColumnSchema } from 'components/common/EntityDataTable';
import { Link } from 'components/common/router';
import DataNodeStatusCell from 'components/datanode/DataNodeList/DataNodeStatusCell';
import type { DataNode } from 'components/datanode/Types';
import NumberUtils from 'util/NumberUtils';
import Routes from 'routing/Routes';

import RatioIndicator from './RatioIndicator';
import { MetricsColumn, MetricsRow, SecondaryText } from './NodeMetricsLayout';
import type { ClusterDataNode } from './useClusterDataNodes';

const RoleLabel = styled(Label)`
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;

export const DEFAULT_VISIBLE_COLUMNS = ['hostname', 'datanode_status', 'jvm', 'datanode_version', 'opensearch_roles'] as const;

const JVM_WARNING_THRESHOLD = 0.7;
const JVM_DANGER_THRESHOLD = 0.9;

export const createColumnDefinitions = (): Array<ColumnSchema> => [
  { id: 'hostname', title: 'Node', sortable: true },
  { id: 'datanode_status', title: 'State', sortable: true },
  { id: 'jvm', title: 'JVM', sortable: false },
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

const computeRatio = (current: number | undefined | null, max: number | undefined | null) => {
  if (current === undefined || current === null || max === undefined || max === null || max === 0) {
    return undefined;
  }

  return current / max;
};

const renderRatioIndicator = (ratio: number | undefined, warning: number, danger: number) =>
  ratio === undefined ? null : <RatioIndicator ratio={ratio} warningThreshold={warning} dangerThreshold={danger} />;

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
    jvm: {
      renderCell: (_value, entity) => {
        const heapUsed = entity.metrics?.jvmMemoryHeapUsed;
        const heapMax = entity.metrics?.jvmMemoryHeapMax;
        const ratio = computeRatio(heapUsed, heapMax);
        const usedLabel = heapUsed === undefined || heapUsed === null ? '' : NumberUtils.formatBytes(heapUsed);
        const maxLabel = heapMax === undefined || heapMax === null ? '' : NumberUtils.formatBytes(heapMax);
        const sizeLabel = [usedLabel, maxLabel].filter(Boolean).join(' / ');
        const ratioIndicator = renderRatioIndicator(ratio, JVM_WARNING_THRESHOLD, JVM_DANGER_THRESHOLD);

        return (
          <MetricsColumn>
            {sizeLabel && (
              <MetricsRow>
                <span>{sizeLabel}</span>
              </MetricsRow>
            )}
            {ratioIndicator && <MetricsRow>{ratioIndicator}</MetricsRow>}
          </MetricsColumn>
        );
      },
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
