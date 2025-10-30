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
import DataNodeStatusCell from 'components/datanode/DataNodeList/DataNodeStatusCell';
import Routes from 'routing/Routes';
import DataNodeActions from 'components/datanode/DataNodeList/DataNodeActions';
import type { Column, ColumnRenderers } from 'components/common/EntityDataTable';
import type { DataNode } from 'components/datanode/Types';
import useDataNodes from 'components/datanode/hooks/useDataNodes';
import type { SearchParams } from 'stores/PaginationTypes';

import ClusterNodesSectionWrapper from './ClusterNodesSectionWrapper';

const RoleLabel = styled(Label)`
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;

const DEFAULT_VISIBLE_COLUMNS = ['node', 'type', 'role', 'state'] as const;

const getRoleLabels = (roles: Array<string>) =>
  roles.map((role) => (
    <span key={role}>
      <RoleLabel bsSize="xs">{role}</RoleLabel>&nbsp;
    </span>
  ));

const getDataNodeRoles = (dataNode: DataNode) =>
  dataNode.opensearch_roles?.map((currentRole) => currentRole.trim()).filter(Boolean) ?? [];

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

  const columnRenderers = useMemo<ColumnRenderers<DataNode>>(
    () => ({
      attributes: {
        node: {
          renderCell: (_value, entity) => {
            const datanodeRouteId = entity.node_id ?? entity.id;
            const nodeName = entity.hostname ?? datanodeRouteId;

            if (!datanodeRouteId) {
              return nodeName;
            }

            return <Link to={Routes.SYSTEM.CLUSTER.DATANODE_SHOW(datanodeRouteId)}>{nodeName}</Link>;
          },
        },
        type: {
          renderCell: (_value, _entity) => 'Data Node - OpenSearch',
        },
        role: {
          renderCell: (_value, entity) => getRoleLabels(getDataNodeRoles(entity)),
        },
        state: {
          renderCell: (_value, entity) => <DataNodeStatusCell dataNode={entity} />,
        },
      },
    }),
    [],
  );

  const dataNodes = dataNodesResponse?.list ?? [];

  const handleColumnsChange = useCallback((newColumns: Array<string>) => {
    if (!newColumns.length) {
      return;
    }

    setVisibleColumns(newColumns);
  }, []);

  const handleSortChange = useCallback(() => {}, []);
  const renderActions = useCallback(
    (entity: DataNode) => <DataNodeActions dataNode={entity} refetch={refetch} />,
    [refetch],
  );

  return (
    <ClusterNodesSectionWrapper title="Data Nodes" headerLeftSection={isInitialLoading && <Spinner />}>
      <EntityDataTable<DataNode>
        entities={dataNodes}
        visibleColumns={visibleColumns}
        columnsOrder={columnsOrder}
        onColumnsChange={handleColumnsChange}
        onSortChange={handleSortChange}
        entityAttributesAreCamelCase
        entityActions={renderActions}
        columnDefinitions={columnDefinitions}
        columnRenderers={columnRenderers}
      />
    </ClusterNodesSectionWrapper>
  );
};

export default DataNodesExpandable;
