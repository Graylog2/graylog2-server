'use strict';

var React = require('react');

var DataTableElement = React.createClass({
    render() {
        return this.props.formatter(this.props.element);
    }
});

var DataTable = React.createClass({
    getInitialState() {
        return {
            headers: this.props.headers,
            rows: this.props.rows
        };
    },
    componentWillReceiveProps(newProps) {
        this.setState({
            headers: newProps.headers,
            rows: newProps.rows
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
        var sortedDataRows = this.state.rows.sort((a, b) => {
            return a[this.props.sortByKey].localeCompare(b[this.props.sortByKey]);
        });
        var formattedDataRows = sortedDataRows.map((row) => {
            i++;
            return <DataTableElement key={"row-" + i} element={row} formatter={this.props.dataRowFormatter}/>;
        });

        return formattedDataRows;
    },
    render() {
        return (
            <div className="row">
                <div className="col-md-12">
                    <div id={this.props.id} className="data-table">
                        <table className="table table-striped">
                            <thead>
                            {this.getFormattedHeaders()}
                            </thead>
                            <tbody>
                            {this.getFormattedDataRows()}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = DataTable;