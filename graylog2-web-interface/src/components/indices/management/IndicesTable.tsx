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
import * as React from 'react';

import { PaginatedEntityTable } from 'components/common';
import NoSearchResult from 'components/common/NoSearchResult';
import type { MiddleSectionProps } from 'components/common/PaginatedEntityTable/PaginatedEntityTable';
import type { SearchParams } from 'stores/PaginationTypes';

import { canActOnAny } from './actions';
import { fetchIndices, indicesKeyFn } from './fetchIndices';
import type { IndexRow } from './fetchIndices';
import { createColumnRenderers, DEFAULT_DISPLAYED_COLUMNS } from './IndexColumnRenderers';
import IndexBulkActions from './IndexBulkActions';
import IndexRowActions from './IndexRowActions';
import useCanRun from './useCanRun';
import useIndexOverview from './useIndexOverview';

const TABLE_LAYOUT = {
  entityTableId: 'index_management',
  defaultSort: { attributeId: 'index', direction: 'asc' as const },
  defaultDisplayedAttributes: DEFAULT_DISPLAYED_COLUMNS,
  defaultPageSize: 20,
  defaultColumnOrder: DEFAULT_DISPLAYED_COLUMNS,
};

const REFETCH_INTERVAL_MS = 30000;

// While the allocation panel is open, the list waits for something to narrow it, so opening the panel doesn't
// suddenly make the page much longer.
const WaitingForNarrowing = ({ searchParams }: MiddleSectionProps) =>
  searchParams.query || searchParams.filters?.size ? null : (
    <NoSearchResult>Pick an index in Shard allocation above, or search or filter, to list indices here.</NoSearchResult>
  );

type Props = {
  waitForNarrowing: boolean;
  topRightCol: React.ReactNode;
};

const IndicesTable = ({ waitForNarrowing, topRightCol }: Props) => {
  const can = useCanRun();
  const { indices } = useIndexOverview();
  const renderActions = (index: IndexRow) => <IndexRowActions index={index} />;

  return (
    <PaginatedEntityTable<IndexRow>
      humanName="indices"
      tableLayout={TABLE_LAYOUT}
      fetchEntities={(searchParams: SearchParams) => fetchIndices(searchParams, waitForNarrowing)}
      keyFn={(searchParams: SearchParams) => indicesKeyFn(searchParams, waitForNarrowing)}
      fetchOptions={{ refetchInterval: REFETCH_INTERVAL_MS }}
      columnRenderers={createColumnRenderers()}
      entityActions={renderActions}
      bulkSelection={canActOnAny(indices, can) ? { actions: <IndexBulkActions /> } : undefined}
      entityAttributesAreCamelCase={false}
      searchPlaceholder="Filter by index or index set"
      topRightCol={topRightCol}
      middleSection={waitForNarrowing ? WaitingForNarrowing : undefined}
    />
  );
};

export default IndicesTable;
