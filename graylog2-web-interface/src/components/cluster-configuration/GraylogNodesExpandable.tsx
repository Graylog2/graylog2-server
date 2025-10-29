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
import JournalState from 'components/nodes/JournalState';
import type { Column, ColumnRenderers } from 'components/common/EntityDataTable';

import useGraylogNodes from './useGraylogNodes';
import type { GraylogNode } from './useGraylogNodes';
import GraylogNodeStatusLabel from './GraylogNodeStatusLabel';
import GraylogNodeActions from './GraylogNodeActions';
import JvmHeapUsageText from './JvmHeapUsageText';
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

const DEFAULT_VISIBLE_COLUMNS = ['node', 'type', 'role', 'state'] as const;

type GraylogNodeEntity = {
  id: string;
  node: string;
  type: string;
  role: string;
  state: GraylogNode;
  nodeInfo: GraylogNode;
};

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

const GraylogNodesExpandable = () => {
  const { nodes: graylogNodes, isLoading } = useGraylogNodes();
  const columnsOrder = useMemo<Array<string>>(() => [...DEFAULT_VISIBLE_COLUMNS], []);
  const [visibleColumns, setVisibleColumns] = useState<Array<string>>([...DEFAULT_VISIBLE_COLUMNS]);

  const columnDefinitions = useMemo<Array<Column>>(
    () => [
      { id: 'node', title: 'Node' },
      { id: 'type', title: 'Type' },
      { id: 'role', title: 'Role' },
      { id: 'state', title: 'State' },
    ],
    [],
  );

  const columnRenderers = useMemo<ColumnRenderers<GraylogNodeEntity>>(
    () => ({
      attributes: {
        node: {
          renderCell: (_value, entity) => {
            const nodeId = entity.nodeInfo.node_id;

            return (
              <NodePrimary>
                {nodeId ? <Link to={Routes.SYSTEM.CLUSTER.NODE_SHOW(nodeId)}>{entity.node}</Link> : entity.node}
                {nodeId && (
                  <>
                    <SecondaryText>
                      <JournalState nodeId={nodeId} />
                    </SecondaryText>
                    <SecondaryText>
                      <JvmHeapUsageText nodeId={nodeId} />
                    </SecondaryText>
                  </>
                )}
              </NodePrimary>
            );
          },
        },
        role: {
          renderCell: (_value, entity) => getRoleLabels(entity.role),
        },
        state: {
          renderCell: (_value, entity) => <GraylogNodeStatusLabel node={entity.nodeInfo} />,
        },
      },
    }),
    [],
  );

  const graylogNodeEntities = useMemo<ReadonlyArray<GraylogNodeEntity>>(
    () =>
      graylogNodes.map((graylogNode) => ({
        id: graylogNode.nodeInfo.node_id ?? graylogNode.nodeName,
        node: graylogNode.nodeName,
        type: graylogNode.type,
        role: graylogNode.role,
        state: graylogNode.nodeInfo,
        nodeInfo: graylogNode.nodeInfo,
      })),
    [graylogNodes],
  );

  const handleColumnsChange = useCallback((newColumns: Array<string>) => {
    if (!newColumns.length) {
      return;
    }

    setVisibleColumns(newColumns);
  }, []);

  const handleSortChange = useCallback(() => {}, []);

  const renderActions = useCallback((entity: GraylogNodeEntity) => <GraylogNodeActions node={entity.nodeInfo} />, []);

  if (!graylogNodeEntities.length && !isLoading) {
    return null;
  }

  return (
    <ClusterNodesSectionWrapper title="Graylog Nodes" headerLeftSection={isLoading && <Spinner />}>
      <EntityDataTable<GraylogNodeEntity>
        entities={graylogNodeEntities}
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
