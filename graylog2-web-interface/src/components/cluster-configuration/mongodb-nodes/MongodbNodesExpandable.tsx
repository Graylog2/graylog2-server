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
import React, { useMemo, useState } from 'react';

import { PaginatedEntityTable } from 'components/common';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import type { FetchOptions } from 'components/common/PaginatedEntityTable/useFetchEntities';

import {
  createColumnDefinitions,
  createColumnRenderers,
  DEFAULT_VISIBLE_COLUMNS,
} from './MongodbNodesColumnConfiguration';
import type { MongodbNode } from './fetchClusterMongodbNodes';
import { clusterMongodbNodesKeyFn, fetchMongodbNodes } from './fetchClusterMongodbNodes';

import ClusterNodesSectionWrapper from '../shared-components/ClusterNodesSectionWrapper';

type Props = {
  collapsible?: boolean;
  searchQuery?: string;
  onSelectNodeType?: () => void;
  pageSizeLimit?: number;
  refetchInterval?: number;
};

const MongodbNodesExpandable = ({
  collapsible = true,
  searchQuery = '',
  onSelectNodeType = undefined,
  pageSizeLimit = undefined,
  refetchInterval = undefined,
}: Props) => {
  const [totalMongodbNodes, setTotalMongodbNodes] = useState<number | undefined>(undefined);

  const columnSchemas = useMemo<Array<ColumnSchema>>(() => createColumnDefinitions(), []);
  const columnRenderers = useMemo(() => createColumnRenderers(), []);

  const tableLayout = useMemo(
    () => ({
      entityTableId: 'cluster-mongodb-nodes',
      defaultSort: { attributeId: 'name', direction: 'asc' as const },
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
      title="MongoDB Nodes"
      titleCount={totalMongodbNodes}
      onTitleCountClick={onSelectNodeType ?? null}
      collapsible={collapsible}>
      <PaginatedEntityTable<MongodbNode>
        tableLayout={tableLayout}
        fetchEntities={fetchMongodbNodes}
        keyFn={clusterMongodbNodesKeyFn}
        additionalAttributes={columnSchemas}
        columnRenderers={columnRenderers}
        entityAttributesAreCamelCase={false}
        humanName="MongoDB Nodes"
        externalSearch={externalSearch}
        fetchOptions={fetchOptions}
        onDataLoaded={(data) => setTotalMongodbNodes(data.pagination?.total ?? data.list.length)}
        withoutURLParams
      />
    </ClusterNodesSectionWrapper>
  );
};

export default MongodbNodesExpandable;
