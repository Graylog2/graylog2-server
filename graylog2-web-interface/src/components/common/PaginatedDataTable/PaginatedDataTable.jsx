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
// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';

import type { Pagination } from 'stores/PaginationTypes';
import DataTable from 'components/common/DataTable';
import PaginatedList from 'components/common/PaginatedList';

import Filter from './Filter';

const DEFAULT_PAGINATION = {
  page: 1,
  perPage: 10,
  query: '',
};

const _paginatedRows = (rows: Array<mixed>, perPage: number, currentPage: number) => {
  const begin = (perPage * (currentPage - 1));
  const end = begin + perPage;

  return rows.slice(begin, end);
};

type Props = {
  /** Element id to use in the table container */
  id: string,
  /** Array of objects to be rendered in the table. The render of those values is controlled by `dataRowFormatter`. */
  rows: Array<mixed>,
  /** Initial pagination. */
  pagination: Pagination,
  /** Label to use next to the suggestions for the data filter input. */
  filterBy?: string,
  /** List of object keys to use as filter in the data filter input. Use an empty array to disable data filter. */
  filterKeys?: Array<string>,
  /** Label to use next to the data filter input. */
  filterLabel?: string,
  /** Object key that should be used to display data in the data filter input. */
  displayKey?: string,
};

/**
 * Component that renders a paginated data table. Should only be used for lists which are not already paginated.
 * If you want to display a lists which gets paginated by the backend, wrap use the DataTable in combination with the PaginatedList.
 */
const PaginatedDataTable = ({ rows = [], pagination: initialPagination, filterKeys, filterLabel, displayKey, filterBy, id, ...rest }: Props) => {
  const [{ perPage, page }, setPagination] = useState(initialPagination);
  const [filteredRows, setFilteredRows] = useState(rows);
  const paginatedRows = _paginatedRows(filteredRows, perPage, page);

  useEffect(() => {
    setFilteredRows(rows);
    setPagination(initialPagination);
  }, [rows, initialPagination]);

  const _onPageChange = (newPage, newPerPage) => {
    setPagination({ page: newPage, perPage: newPerPage });
  };

  const _resetPagination = () => {
    setPagination({ perPage, page: initialPagination.page });
  };

  return (
    <PaginatedList totalItems={filteredRows.length}
                   pageSize={perPage}
                   activePage={page}
                   onChange={_onPageChange}
                   showPageSizeSelect>
      <DataTable {...rest}
                 id={id}
                 customFilter={(
                   <Filter id={id}
                           filterKeys={filterKeys}
                           setFilteredRows={setFilteredRows}
                           rows={rows}
                           resetPagination={_resetPagination}
                           displayKey={displayKey}
                           filterBy={filterBy}
                           filterLabel={filterLabel} />
                 )}
                 rows={paginatedRows} />
    </PaginatedList>
  );
};

PaginatedDataTable.defaultProps = {
  displayKey: undefined,
  filterKeys: undefined,
  filterLabel: 'Filter',
  filterBy: undefined,
  pagination: DEFAULT_PAGINATION,
};

PaginatedDataTable.propTypes = {
  pagination: PropTypes.object,
};

export default PaginatedDataTable;
