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
import React, { useCallback, useMemo } from 'react';
import styled from 'styled-components';

import { EntityDataTable, NoSearchResult, Spinner } from 'components/common';
import type { ColumnSchema } from 'components/common/EntityDataTable';

import useClusterGraylogNodes from './useClusterGraylogNodes';
import type { GraylogNode } from './useClusterGraylogNodes';
import GraylogNodeActions from './GraylogNodeActions';
import { createColumnDefinitions, createColumnRenderers } from './GraylogNodesColumnConfiguration';
import useClusterGraylogNodesTableLayout from './useClusterGraylogNodesTableLayout';

import ClusterNodesSectionWrapper from '../shared-components/ClusterNodesSectionWrapper';

const EmptyState = styled(NoSearchResult)`
  margin-top: ${({ theme }) => theme.spacings.xl};
`;

type Props = {
  collapsible?: boolean;
  searchQuery?: string;
  onSelectNodeType?: () => void;
  pageSizeLimit?: number;
  refetchInterval?: number | false;
};

const GraylogNodesExpandable = ({
  collapsible = true,
  searchQuery = '',
  onSelectNodeType = undefined,
  pageSizeLimit = undefined,
  refetchInterval = undefined,
}: Props) => {
  const {
    defaultDisplayedColumns,
    defaultColumnOrder,
    layoutPreferences,
    searchParams,
    isLoadingLayout,
    handleLayoutPreferencesChange,
    handleSortChange,
  } = useClusterGraylogNodesTableLayout(searchQuery, pageSizeLimit);
  const { nodes: graylogNodes, total: totalGraylogNodes, isLoading } = useClusterGraylogNodes(searchParams, { refetchInterval });

  const columnSchemas = useMemo<Array<ColumnSchema>>(() => createColumnDefinitions(), []);
  const columnRenderers = useMemo(() => createColumnRenderers(), []);

  const renderActions = useCallback((entity: GraylogNode) => <GraylogNodeActions node={entity} />, []);

  return (
    <ClusterNodesSectionWrapper
      title="Graylog Nodes"
      titleCount={totalGraylogNodes}
      onTitleCountClick={onSelectNodeType ?? null}
      headerLeftSection={(isLoading || isLoadingLayout) && <Spinner />}
      collapsible={collapsible}>
      {(() => {
        if (isLoading || isLoadingLayout) {
          return <Spinner />;
        }
        if (graylogNodes.length === 0) {
          return <EmptyState>No Graylog Nodes found.</EmptyState>;
        }

        return (
          <EntityDataTable<GraylogNode>
            entities={graylogNodes}
            defaultDisplayedColumns={defaultDisplayedColumns}
            defaultColumnOrder={defaultColumnOrder}
            layoutPreferences={layoutPreferences}
            onLayoutPreferencesChange={handleLayoutPreferencesChange}
            onSortChange={handleSortChange}
            activeSort={searchParams.sort}
            entityAttributesAreCamelCase={false}
            entityActions={renderActions}
            columnSchemas={columnSchemas}
            columnRenderers={columnRenderers}
            actionsCellWidth={160}
          />
        );
      })()}
    </ClusterNodesSectionWrapper>
  );
};

export default GraylogNodesExpandable;
