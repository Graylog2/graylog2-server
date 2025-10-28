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
import { EntityDataTable, Section, Spinner } from 'components/common';
import { Link } from 'components/common/router';
import DataNodeStatusCell from 'components/datanode/DataNodeList/DataNodeStatusCell';
import Routes from 'routing/Routes';
import DataNodeActions from 'components/datanode/DataNodeList/DataNodeActions';
import type { Column, ColumnRenderers } from 'components/common/EntityDataTable';
import type { DataNode } from 'components/datanode/Types';
import useDataNodes from 'components/datanode/hooks/useDataNodes';
import type { SearchParams } from 'stores/PaginationTypes';

const RoleLabel = styled(Label)`
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;

const DEFAULT_VISIBLE_COLUMNS = ['node', 'type', 'role', 'state'] as const;

type DataNodeEntity = {
  id: string;
  node: string;
  type: string;
  role: Array<string>;
  state: DataNode;
  nodeInfo: DataNode;
};

const getRoleLabels = (roles: Array<string>) =>
  roles.map((role) => (
    <span key={role}>
      <RoleLabel bsSize="xs">{role}</RoleLabel>&nbsp;
    </span>
  ));

const DEFAULT_SEARCH_PARAMS: SearchParams = {
  query: '',
  page: 1,
  pageSize: 0,
  sort: { attributeId: 'hostname', direction: 'asc' },
};

const DataNodesExpandable = () => {
  const columnsOrder = useMemo<Array<string>>(() => [...DEFAULT_VISIBLE_COLUMNS], []);
  const [visibleColumns, setVisibleColumns] = useState<Array<string>>([...DEFAULT_VISIBLE_COLUMNS]);
  const searchParams = DEFAULT_SEARCH_PARAMS;
  const {
    data: dataNodesResponse,
    refetch,
    isInitialLoading,
  } = useDataNodes(searchParams);
  const columnDefinitions = useMemo<Array<Column>>(
    () => [
      { id: 'node', title: 'Node' },
      { id: 'type', title: 'Type' },
      { id: 'role', title: 'Role' },
      { id: 'state', title: 'State' },
    ],
    [],
  );

  const columnRenderers = useMemo<ColumnRenderers<DataNodeEntity>>(
    () => ({
      attributes: {
        node: {
          renderCell: (_value, entity) => {
            const datanodeRouteId = entity.nodeInfo.node_id ?? entity.nodeInfo.id;

            if (!datanodeRouteId) {
              return entity.node;
            }

            return <Link to={Routes.SYSTEM.CLUSTER.DATANODE_SHOW(datanodeRouteId)}>{entity.node}</Link>;
          },
        },
        role: {
          renderCell: (_value, entity) => getRoleLabels(entity.role),
        },
        state: {
          renderCell: (_value, entity) => <DataNodeStatusCell dataNode={entity.nodeInfo} />,
        },
      },
    }),
    [],
  );

  const dataNodeEntities = useMemo(
    () => {
      const dataNodes = dataNodesResponse?.list || [];

      return dataNodes.map((dataNode, index) => {
        const roles = dataNode?.opensearch_roles?.map((currentRole) => currentRole.trim()).filter(Boolean) ?? [];
        const nodeName = dataNode?.hostname ?? dataNode?.node_id ?? dataNode?.cluster_address ?? dataNode?.rest_api_address;
        const nodeId =
          dataNode?.node_id ??
          dataNode?.id ??
          dataNode?.hostname ??
          dataNode?.cluster_address ??
          dataNode?.rest_api_address ??
          `data-node-${index}`;

        return {
          id: nodeId,
          node: nodeName ?? nodeId,
          type: 'Data Node - OpenSearch',
          role: roles,
          state: dataNode,
          nodeInfo: dataNode,
        };
      });
    },
    [dataNodesResponse],
  );

  const handleColumnsChange = useCallback((newColumns: Array<string>) => {
    if (!newColumns.length) {
      return;
    }

    setVisibleColumns(newColumns);
  }, []);

  const handleSortChange = useCallback(() => {}, []);
  const renderActions = useCallback(
    (entity: DataNodeEntity) => <DataNodeActions dataNode={entity.nodeInfo} refetch={refetch} />,
    [refetch],
  );

  return (
    <Section title="Data Nodes" collapsible>
      {isInitialLoading && <Spinner />}
      <EntityDataTable<DataNodeEntity>
        entities={dataNodeEntities}
        visibleColumns={visibleColumns}
        columnsOrder={columnsOrder}
        onColumnsChange={handleColumnsChange}
        onSortChange={handleSortChange}
        entityAttributesAreCamelCase
        entityActions={renderActions}
        columnDefinitions={columnDefinitions}
        columnRenderers={columnRenderers}
      />
    </Section>
  );
};

export default DataNodesExpandable;
