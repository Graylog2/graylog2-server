import React from 'react';
import DataTableElement from './DataTableElement';
import { TypeAheadDataFilter } from 'components/common';

const DataTable = React.createClass({
  propTypes: {
    children: React.PropTypes.node,
    className: React.PropTypes.string,
    rowClassName: React.PropTypes.string,
    displayKey: React.PropTypes.string,
    dataRowFormatter: React.PropTypes.func.isRequired,
    filterBy: React.PropTypes.string,
    filterLabel: React.PropTypes.string.isRequired,
    filterKeys: React.PropTypes.array.isRequired,
    filterSuggestions: React.PropTypes.array,
    headerCellFormatter: React.PropTypes.func.isRequired,
    headers: React.PropTypes.array.isRequired,
    id: React.PropTypes.string,
    noDataText: React.PropTypes.oneOfType([React.PropTypes.string, React.PropTypes.node]),
    rows: React.PropTypes.array.isRequired,
    sortByKey: React.PropTypes.string,
  },
  getDefaultProps() {
    return {
      filterSuggestions: [],
      displayKey: 'value',
      noDataText: 'No data available.',
      rowClassName: '',
    };
  },
  getInitialState() {
    return {
      headers: this.props.headers,
      rows: this.props.rows,
      filteredRows: this.props.rows,
    };
  },
  componentWillReceiveProps(newProps) {
    this.setState({
      headers: newProps.headers,
      rows: newProps.rows,
      filteredRows: newProps.rows,
    });
  },
  getFormattedHeaders() {
    let i = 0;
    const formattedHeaders = this.state.headers.map((header) => {
      const el = <DataTableElement key={`header-${i}`} element={header} index={i} formatter={this.props.headerCellFormatter} />;
      i++;
      return el;
    });

    return <tr>{formattedHeaders}</tr>;
  },
  getFormattedDataRows() {
    let i = 0;
    let sortedDataRows = this.state.filteredRows;
    if (this.props.sortByKey) {
      sortedDataRows = sortedDataRows.sort((a, b) => {
        return a[this.props.sortByKey].localeCompare(b[this.props.sortByKey]);
      });
    }
    const formattedDataRows = sortedDataRows.map((row) => {
      const el = <DataTableElement key={`row-${i}`} element={row} index={i} formatter={this.props.dataRowFormatter} />;
      i++;
      return el;
    });

    return formattedDataRows;
  },
  filterDataRows(filteredRows) {
    this.setState({ filteredRows });
  },
  render() {
    let filter;
    if (this.props.filterKeys.length !== 0) {
      filter = (
        <div className="row">
          <div className="col-md-8">
            <TypeAheadDataFilter label={this.props.filterLabel}
                                 data={this.state.rows}
                                 displayKey={this.props.displayKey}
                                 filterBy={this.props.filterBy}
                                 filterSuggestions={this.props.filterSuggestions}
                                 searchInKeys={this.props.filterKeys}
                                 onDataFiltered={this.filterDataRows} />
          </div>
          <div className="col-md-4">
            {this.props.children}
          </div>
        </div>
      );
    }

    let data;
    if (this.state.rows.length === 0) {
      data = <p>{this.props.noDataText}</p>;
    } else if (this.state.filteredRows.length === 0) {
      data = <p>Filter does not match any data.</p>;
    } else {
      data = (
        <table className={`table ${this.props.className}`}>
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
        <div className={`row ${this.props.rowClassName}`}>
          <div className="col-md-12">
            <div id={this.props.id} className="data-table table-responsive">
              {data}
            </div>
          </div>
        </div>
      </div>
    );
  },
});

export default DataTable;
