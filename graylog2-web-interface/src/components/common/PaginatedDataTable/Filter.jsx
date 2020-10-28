// @flow strict
import * as React from 'react';

import DataTableFilter from 'components/common/DataTable/Filter';

type Props = {
  setFilteredRows: (Array<mixed>) => void,
  resetPagination: () => void,
  rows: Array<mixed>,
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

export default Filter;
