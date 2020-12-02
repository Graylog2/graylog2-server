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

import DataTableFilter from 'components/common/DataTable/Filter';

type Props = {
  setFilteredRows: (row: Array<unknown>) => void,
  resetPagination: () => void,
  rows: Array<unknown>,
  id?: string,
  filterKeys?: unknown,
  displayKey?: unknown,
  filterBy?: unknown,
  filterLabel?: unknown,
};

const Filter = ({ setFilteredRows, resetPagination, rows, ...filterProps }: Props) => {
  const onDataFiltered = (newFilteredGroups, filterText) => {
    if (filterText && filterText !== '') {
      setFilteredRows(newFilteredGroups);
    } else {
      setFilteredRows(rows);
    }

    resetPagination();
  };

  return (
    <DataTableFilter {...filterProps}
                     rows={rows}
                     onDataFiltered={onDataFiltered} />
  );
};

Filter.defaultProps = {
  id: undefined,
  filterKeys: undefined,
  displayKey: undefined,
  filterBy: undefined,
  filterLabel: undefined,
};

export default Filter;
