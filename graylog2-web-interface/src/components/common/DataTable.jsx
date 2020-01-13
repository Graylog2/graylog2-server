import PropTypes from 'prop-types';
import React from 'react';
import { isEqual } from 'lodash';
import TypeAheadDataFilter from 'components/common/TypeAheadDataFilter';
import DataTableElement from './DataTableElement';

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

  componentDidUpdate(previousProps) {
    // We update the state with row if the filterKeys is empty other than that typeahead is handling the state
    const { filterKeys, rows } = this.props;
    if (filterKeys.length === 0 && !isEqual(previousProps.rows, rows)) {
      this._updateState();
    }
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
    let { filteredRows: sortedDataRows } = this.state;
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

  _updateState() {
    const { rows } = this.props;
    this.setState({
      filteredRows: rows,
    });
  }

  render() {
    let filter;
    const {
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
    const { filteredRows } = this.state;
    if (filterKeys.length !== 0) {
      filter = (
        <div className="row">
          <div className="col-md-8">
            <TypeAheadDataFilter id={`${id}-data-filter`}
                                 label={filterLabel}
                                 data={rows}
                                 displayKey={displayKey}
                                 filterBy={filterBy}
                                 filterSuggestions={filterSuggestions}
                                 searchInKeys={filterKeys}
                                 onDataFiltered={this.filterDataRows} />
          </div>
          <div className="col-md-4">
            {children}
          </div>
        </div>
      );
    }

    let data;
    if (rows.length === 0) {
      data = <p>{noDataText}</p>;
    } else if (filteredRows.length === 0) {
      data = <p>Filter does not match any data.</p>;
    } else {
      data = (
        <table className={`table ${className}`}>
          <thead>
            {this.getFormattedHeaders()}
          </thead>
          <tbody>
            {this.getFormattedDataRows()}
          </tbody>
        </table>
      );
    }

    return (
      <div>
        {filter}
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
