'use strict';

var React = require('react');

var DataFilter = require('./DataFilter');

var DataTableElement = React.createClass({
    render() {
        return this.props.formatter(this.props.element);
    }
});

var DataTable = React.createClass({
    getInitialState() {
        return {
            headers: this.props.headers,
            rows: this.props.rows,
            filteredRows: this.props.rows
        };
    },
    componentWillReceiveProps(newProps) {
        this.setState({
            headers: newProps.headers,
            rows: newProps.rows,
            filteredRows: newProps.rows
        });
    },
    getFormattedHeaders() {
        var i = 0;
        var formattedHeaders = this.state.headers.map((header) => {
            i++;
            return <DataTableElement key={"header-" + i} element={header} formatter={this.props.headerCellFormatter}/>;
        });

        return <tr>{formattedHeaders}</tr>;
    },
    getFormattedDataRows() {
        var i = 0;
        var sortedDataRows = this.state.filteredRows.sort((a, b) => {
            return a[this.props.sortByKey].localeCompare(b[this.props.sortByKey]);
        });
        var formattedDataRows = sortedDataRows.map((row) => {
            i++;
            return <DataTableElement key={"row-" + i} element={row} formatter={this.props.dataRowFormatter}/>;
        });

        return formattedDataRows;
    },
    filterDataRows(filteredRows) {
        this.setState({filteredRows: filteredRows});
    },
    render() {
        var filter;
        if (this.props.filterKeys.length !== 0) {
            filter = (
                <div className="row">
                    <div className="col-md-4">
                        <DataFilter label={this.props.filterLabel}
                                    data={this.state.rows}
                                    filterKeys={this.props.filterKeys}
                                    onFilterUpdate={this.filterDataRows}/>
                    </div>
                </div>
            );
        }

        var data;
        if (this.state.rows.length === 0) {
            data = <p>No data available.</p>;
        } else if (this.state.filteredRows.length === 0) {
            data = <p>Filter does not match any data.</p>;
        } else {
            data = (
                <table className={"table " + this.props.className}>
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
                <div className="row">
                    <div className="col-md-12">
                        <div id={this.props.id} className="data-table table-responsive">
                            {data}
                        </div>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = DataTable;