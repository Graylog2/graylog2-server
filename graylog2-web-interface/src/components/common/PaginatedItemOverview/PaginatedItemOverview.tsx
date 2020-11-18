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
import { useEffect, useState } from 'react';
import * as Immutable from 'immutable';

import PaginatedList, { INITIAL_PAGE } from 'components/common/PaginatedList';
import SearchForm from 'components/common/SearchForm';
import Spinner from 'components/common/Spinner';
import EmptyResult from 'components/common/EmptyResult';
import type { ListPagination, Pagination } from 'stores/PaginationTypes';

import PaginatedItem from './PaginatedItem';

export type DescriptiveItem = {
  id: string;
  name: string;
  description: string;
};

export type PaginatedListType = {
  list: Immutable.List<DescriptiveItem>,
  pagination: ListPagination,
};

type Props = {
  noDataText?: string,
  onLoad: (pagination: Pagination, isSubscribed: boolean) => Promise<PaginatedListType>,
  overrideList?: PaginatedListType,
  onDeleteItem?: (descriptiveItem: DescriptiveItem) => void,
  queryHelper?: React.ReactNode,
};

const pageSizes = [5, 10, 30];
export const DEFAULT_PAGINATION = { page: INITIAL_PAGE, perPage: pageSizes[0], query: '' };

const PaginatedItemOverview = ({ onLoad, overrideList, onDeleteItem, queryHelper, noDataText }: Props) => {
  const [paginatedList, setPaginatedList] = useState<PaginatedListType | undefined>();
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  useEffect(() => overrideList && setPaginatedList(overrideList), [overrideList]);

  useEffect(() => {
    let isSubscribed = true;

    onLoad(pagination, isSubscribed).then((response) => {
      if (isSubscribed) {
        setPaginatedList(response);
      }
    });

    return () => { isSubscribed = false; };
  }, [pagination, onLoad]);

  if (!paginatedList) {
    return <Spinner />;
  }

  const emptyResult = <EmptyResult>{noDataText}</EmptyResult>;
  let itemList;

  if (paginatedList.list && paginatedList.list.size >= 1) {
    itemList = paginatedList.list.toArray().map((item) => <PaginatedItem key={item.id} onDeleteItem={onDeleteItem} item={item} />);
  }

  return (
    <PaginatedList onChange={(newPage, newPerPage) => setPagination({ ...pagination, page: newPage, perPage: newPerPage })}
                   pageSize={pagination.perPage}
                   totalItems={paginatedList?.pagination?.total ?? 0}
                   pageSizes={pageSizes}
                   activePage={pagination.page}>
      <SearchForm onSearch={(newQuery) => setPagination({ ...pagination, page: INITIAL_PAGE, query: newQuery })}
                  label="Filter"
                  wrapperClass="has-bm"
                  placeholder="Enter query to filter"
                  queryHelpComponent={queryHelper}
                  searchButtonLabel="Filter" />
      <div>
        {itemList ?? emptyResult}
      </div>
    </PaginatedList>
  );
};

PaginatedItemOverview.defaultProps = {
  onDeleteItem: undefined,
  overrideList: undefined,
  noDataText: 'No items found to display.',
  queryHelper: undefined,
};

export default PaginatedItemOverview;
