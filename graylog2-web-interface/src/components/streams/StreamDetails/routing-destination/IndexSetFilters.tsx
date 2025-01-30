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

import type { PaginatedList as PaginatedListType } from 'stores/PaginationTypes';
import FilterRulesList from 'components/streams/StreamDetails/output-filter/FilterRuleList';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';

type Props = {
  streamId: string,
  paginatedFilters: PaginatedListType<StreamOutputFilterRule>,
  onPaginationChange: (newPage: number, newPerPage: number) => void,
};

const IndexSetFilters = ({ streamId, paginatedFilters, onPaginationChange }: Props) => (
  <FilterRulesList streamId={streamId}
                   paginatedFilters={paginatedFilters}
                   destinationType="indexer"
                   onPaginationChange={onPaginationChange}
                   requiredPermissions={['indexsets:edit']} />

);

export default IndexSetFilters;
