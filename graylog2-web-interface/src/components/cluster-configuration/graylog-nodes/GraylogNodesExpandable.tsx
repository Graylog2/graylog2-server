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

import { PaginatedEntityTable } from 'components/common';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import type { FetchOptions } from 'components/common/PaginatedEntityTable/useFetchEntities';
import useProductName from 'brand-customization/useProductName';

import type { ClusterGraylogNode } from './fetchClusterGraylogNodes';
import { clusterGraylogNodesKeyFn, fetchClusterGraylogNodesWithMetrics } from './fetchClusterGraylogNodes';
import GraylogNodeActions from './GraylogNodeActions';
import {
  createColumnDefinitions,
  createColumnRenderers,
  DEFAULT_VISIBLE_COLUMNS,
} from './GraylogNodesColumnConfiguration';

import ClusterNodesSectionWrapper from '../shared-components/ClusterNodesSectionWrapper';

type Props = {
  collapsible?: boolean;
  searchQuery?: string;
  onSelectNodeType?: () => void;
  pageSizeLimit?: number;
  refetchInterval?: number;
};

const GraylogNodesExpandable = ({
  collapsible = true,
  searchQuery = '',
  onSelectNodeType = undefined,
  pageSizeLimit = undefined,
  refetchInterval = undefined,
}: Props) => {
  const productName = useProductName();
  const [totalGraylogNodes, setTotalGraylogNodes] = useState<number | undefined>(undefined);

  const columnSchemas = useMemo<Array<ColumnSchema>>(() => createColumnDefinitions(), []);
  const columnRenderers = useMemo(() => createColumnRenderers(), []);

  const renderActions = useCallback((entity: ClusterGraylogNode) => <GraylogNodeActions node={entity} />, []);
  const tableLayout = useMemo(
    () => ({
      entityTableId: 'cluster-graylog-nodes',
      defaultSort: { attributeId: 'hostname', direction: 'asc' as const },
      defaultDisplayedAttributes: [...DEFAULT_VISIBLE_COLUMNS],
      defaultPageSize: pageSizeLimit ?? 0,
      defaultColumnOrder: [...DEFAULT_VISIBLE_COLUMNS],
    }),
    [pageSizeLimit],
  );
  const externalSearch = useMemo(() => ({ query: searchQuery }), [searchQuery]);
  const fetchOptions = useMemo<FetchOptions>(() => ({ refetchInterval }), [refetchInterval]);

  return (
    <ClusterNodesSectionWrapper
      title={`${productName} Nodes`}
      titleCount={totalGraylogNodes}
      onTitleCountClick={onSelectNodeType ?? null}
      collapsible={collapsible}>
      <PaginatedEntityTable<ClusterGraylogNode>
        tableLayout={tableLayout}
        fetchEntities={fetchClusterGraylogNodesWithMetrics}
        keyFn={clusterGraylogNodesKeyFn}
        additionalAttributes={columnSchemas}
        columnRenderers={columnRenderers}
        entityActions={renderActions}
        entityAttributesAreCamelCase={false}
        humanName={`${productName} Nodes`}
        externalSearch={externalSearch}
        fetchOptions={fetchOptions}
        onDataLoaded={(data) => setTotalGraylogNodes(data.pagination?.total ?? data.list.length)}
        withoutURLParams
      />
    </ClusterNodesSectionWrapper>
  );
};

export default GraylogNodesExpandable;
