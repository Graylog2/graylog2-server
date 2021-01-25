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
import PropTypes from 'prop-types';
import React from 'react';
import { isEqual } from 'lodash';
import styled from 'styled-components';

import { tableCss } from 'components/graylog/Table';

import Filter from './Filter';
import DataTableElement from './DataTableElement';

const StyledTable = styled.table`
  ${tableCss}
`;

const NoData = ({ noDataText }) => {
  if (typeof noDataText === 'string') {
    return (
      <p>
        {noDataText}
      </p>
    );
  }

  return noDataText;
};

/**
 * Component that renders a data table, letting consumers of the component to
 * decide exactly how the data should be rendered. It optionally adds a filter
 * input to the data table by using the the `TypeAheadDataFilter` component.
 */
class DataTable extends React.Component {
  static propTypes = {
    /** Adds a custom children element next to the data filter input. */
    children: PropTypes.node,
    /** Adds a custom class to the table element. */
    className: PropTypes.string,
    /** Overrides the default filter. */
    customFilter: PropTypes.node,
    /** Adds a custom class to the row element. */
    rowClassName: PropTypes.string,
    /** Object key that should be used to display data in the data filter input. */
    displayKey: PropTypes.string,
    /**
     * Function that renders a row in the table. It receives two arguments: the row, and its index.
     * It usually returns a `<tr>` element with the formatted row.
     */
    dataRowFormatter: PropTypes.func.isRequired,
    /** Label to use next to the suggestions for the data filter input. */
    filterBy: PropTypes.string,
    /** Label to use next to the data filter input. */
    filterLabel: PropTypes.string,
    /** List of object keys to use as filter in the data filter input. Use an empty array to disable data filter. */
    filterKeys: PropTypes.array,
    /** Array to use as suggestions in the data filter input. */
    filterSuggestions: PropTypes.array,
    /**
     * Function that renders a single header cell in the table. It receives two arguments: the header, and its index.
     * It usually returns a `<th>` element with the header.
     * Default will wrap the headers in a <th> tag.
     */
    headerCellFormatter: PropTypes.func,
    /** Array of values to be use as headers. The render is controlled by `headerCellFormatter`. */
    headers: PropTypes.array.isRequired,
    /** Element id to use in the table container */
    id: PropTypes.string.isRequired,
    /** Text or element to show when there is no data. */
    noDataText: PropTypes.oneOfType([PropTypes.string, PropTypes.node]),
    /** Array of objects to be rendered in the table. The render of those values is controlled by `dataRowFormatter`. */
    rows: PropTypes.array.isRequired,
    /** Object key to use to sort data table. */
    sortByKey: PropTypes.string,
    /** Function that returns the value used to sort data table. (not used if `sortByKey` is defined as well) */
    sortBy: PropTypes.func,
    /**
     * Indicates whether the table should use a bootstrap responsive table or not:
     * https://getbootstrap.com/docs/3.3/css/#tables-responsive
     *
     * The main reason to disable this is if the table is clipping a dropdown menu or another component in a table edge.
     */
    useResponsiveTable: PropTypes.bool,
  };

  static defaultProps = {
    customFilter: undefined,
    children: undefined,
    className: '',
    filterBy: '',
    filterSuggestions: [],
    filterKeys: [],
    filterLabel: 'Filter',
    displayKey: 'value',
    noDataText: 'No data available.',
    rowClassName: '',
    useResponsiveTable: true,
    headerCellFormatter: (header) => { return (<th>{header}</th>); },
    sortByKey: undefined,
    sortBy: undefined,
  };

  constructor(props) {
    super(props);
    const { rows } = this.props;

    this.state = {
      filteredRows: rows,
    };
  }

  getFormattedHeaders = () => {
    let i = 0;
    const { headerCellFormatter, headers } = this.props;
    const formattedHeaders = headers.map((header) => {
      const el = <DataTableElement key={`header-${i}`} element={header} index={i} formatter={headerCellFormatter} />;

      i += 1;

      return el;
    });

    return <tr>{formattedHeaders}</tr>;
  };

  getFormattedDataRows = () => {
    let i = 0;
    const { sortByKey, sortBy, dataRowFormatter } = this.props;
    let sortedDataRows = this._getEffectiveRows();

    if (sortByKey) {
      sortedDataRows = sortedDataRows.sort((a, b) => {
        return a[sortByKey].localeCompare(b[sortByKey]);
      });
    } else if (sortBy) {
      sortedDataRows = sortedDataRows.sort((a, b) => {
        return sortBy(a).localeCompare(sortBy(b));
      });
    }

    const formattedDataRows = sortedDataRows.map((row) => {
      const el = <DataTableElement key={`row-${i}`} element={row} index={i} formatter={dataRowFormatter} />;

      i += 1;

      return el;
    });

    return formattedDataRows;
  };

  filterDataRows = (filteredRows) => {
    this.setState({ filteredRows });
  };

  _getEffectiveRows = () => {
    const { filteredRows } = this.state;
    const { filterKeys, rows } = this.props;

    return (filterKeys.length === 0 ? rows : filteredRows.filter((row) => rows.some((r) => isEqual(r, row))));
  };

  render() {
    const {
      customFilter,
      filterKeys,
      id,
      filterLabel,
      filterBy,
      displayKey,
      filterSuggestions,
      children,
      noDataText,
      className,
      rowClassName,
      useResponsiveTable,
      rows,
    } = this.props;
    const effectiveRows = this._getEffectiveRows();

    let data;

    if (rows.length === 0) {
      data = <NoData noDataText={noDataText} />;
    } else if (effectiveRows.length === 0) {
      data = <p>Filter does not match any data.</p>;
    } else {
      data = (
        <StyledTable className={`table ${className}`}>
          <thead>
            {this.getFormattedHeaders()}
          </thead>
          <tbody>
            {this.getFormattedDataRows()}
          </tbody>
        </StyledTable>
      );
    }

    return (
      <div>
        {customFilter || (
          <Filter label={filterLabel}
                  rows={rows}
                  id={id}
                  displayKey={displayKey}
                  filterBy={filterBy}
                  filterSuggestions={filterSuggestions}
                  filterKeys={filterKeys}
                  onDataFiltered={this.filterDataRows}>
            {children}
          </Filter>
        )}
        <div className={`row ${rowClassName}`}>
          <div className="col-md-12">
            <div id={id} className={`data-table ${useResponsiveTable ? 'table-responsive' : ''}`}>
              {data}
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default DataTable;
