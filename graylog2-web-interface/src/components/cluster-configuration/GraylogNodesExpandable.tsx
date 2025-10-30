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
import React, { useCallback, useMemo, useState } from 'react';
import styled from 'styled-components';

import { Label } from 'components/bootstrap';
import { EntityDataTable, Spinner } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { Column, ColumnRenderers } from 'components/common/EntityDataTable';
import NumberUtils from 'util/NumberUtils';

import useGraylogNodes from './useGraylogNodes';
import type { GraylogNode } from './useGraylogNodes';
import GraylogNodeStatusLabel from './GraylogNodeStatusLabel';
import GraylogNodeActions from './GraylogNodeActions';
import ClusterNodesSectionWrapper from './ClusterNodesSectionWrapper';

const SecondaryText = styled.div`
  span {
    font-size: small;
  }
`;

const RoleLabel = styled(Label)`
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;

const NodePrimary = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
`;

const DEFAULT_VISIBLE_COLUMNS = ['node', 'type', 'journal', 'jvm', 'role', 'state'] as const;

const getRoleLabels = (roles: string) =>
  roles
    .split(',')
    .map((role) => role.trim())
    .filter(Boolean)
    .map((role) => (
      <span key={role}>
        <RoleLabel bsSize="xs">{role}</RoleLabel>&nbsp;
      </span>
    ));

const getNodeDisplayName = (node: GraylogNode) => {
  const nodeNameParts = [node.short_node_id, node.hostname].filter(Boolean);

  if (nodeNameParts.length) {
    return nodeNameParts.join(' / ');
  }

  return node.node_id ?? node.hostname ?? node.id;
};

const GraylogNodesExpandable = () => {
  const { nodes: graylogNodes, isLoading } = useGraylogNodes();
  const columnsOrder = useMemo<Array<string>>(() => [...DEFAULT_VISIBLE_COLUMNS], []);
  const [visibleColumns, setVisibleColumns] = useState<Array<string>>([...DEFAULT_VISIBLE_COLUMNS]);

  const columnDefinitions = useMemo<Array<Column>>(
    () => [
      { id: 'node', title: 'Node' },
      { id: 'type', title: 'Type' },
      { id: 'role', title: 'Role' },
      { id: 'journal', title: 'Journal' },
      { id: 'jvm', title: 'JVM' },
      { id: 'state', title: 'State' },
    ],
    [],
  );

  const columnRenderers = useMemo<ColumnRenderers<GraylogNode>>(
    () => ({
      attributes: {
        node: {
          renderCell: (_value, entity) => {
            const nodeId = entity.node_id;
            const nodeName = getNodeDisplayName(entity);

            return (
              <NodePrimary>
                {nodeId ? <Link to={Routes.SYSTEM.CLUSTER.NODE_SHOW(nodeId)}>{nodeName}</Link> : nodeName}
              </NodePrimary>
            );
          },
        },
        type: {
          renderCell: () => 'Graylog',
        },
        role: {
          renderCell: (_value, entity) => getRoleLabels(entity.is_leader ? 'Leader' : 'Non-Leader'),
        },
        journal: {
          renderCell: (_value, entity) => {
            const journalCurrent = entity.metrics?.journalSize;
            const journalMax = entity.metrics?.journalMaxSize;
            const ratio = entity.metrics?.journalSizeRatio;

            return (
              <div>
                {`${NumberUtils.formatBytes(journalCurrent)} / ${NumberUtils.formatBytes(journalMax)}`}
                <SecondaryText>
                  <span>
                    {ratio !== undefined && ratio !== null ? NumberUtils.formatPercentage(ratio) : 'N/A'}
                  </span>
                </SecondaryText>
              </div>
            );
          },
        },
        jvm: {
          renderCell: (_value, entity) => {
            const heapUsed = entity.metrics?.jvmMemoryHeapUsed;
            const heapMax = entity.metrics?.jvmMemoryHeapMaxMemory;
            const ratio =
              heapUsed !== undefined && heapUsed !== null && heapMax ? heapUsed / heapMax : undefined;

            return (
              <div>
                {`${NumberUtils.formatBytes(heapUsed)} / ${NumberUtils.formatBytes(heapMax)}`}
                <SecondaryText>
                  <span>{ratio !== undefined ? NumberUtils.formatPercentage(ratio) : 'N/A'}</span>
                </SecondaryText>
              </div>
            );
          },
        },
        state: {
          renderCell: (_value, entity) => <GraylogNodeStatusLabel node={entity} />,
        },
      },
    }),
    [],
  );

  const handleColumnsChange = useCallback((newColumns: Array<string>) => {
    if (!newColumns.length) {
      return;
    }

    setVisibleColumns(newColumns);
  }, []);

  const handleSortChange = useCallback(() => {}, []);

  const renderActions = useCallback((entity: GraylogNode) => <GraylogNodeActions node={entity} />, []);


  return (
    <ClusterNodesSectionWrapper title="Graylog Nodes" headerLeftSection={isLoading && <Spinner />}>
      <EntityDataTable<GraylogNode>
        entities={graylogNodes}
        visibleColumns={visibleColumns}
        columnsOrder={columnsOrder}
        onColumnsChange={handleColumnsChange}
        onSortChange={handleSortChange}
        entityAttributesAreCamelCase
        entityActions={renderActions}
        columnDefinitions={columnDefinitions}
        columnRenderers={columnRenderers}
        actionsCellWidth={160}
      />
    </ClusterNodesSectionWrapper>
  );
};

export default GraylogNodesExpandable;
