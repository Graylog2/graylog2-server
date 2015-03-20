'use strict';

var React = require('react');
var numeral = require('numeral');

var TableRowWrapper = React.createClass({
    render() {
        return (
            <tr>
                <td>{this.props.term}</td>
                <td>{this.props.percentage}</td>
                <td>{this.props.count}</td>
            </tr>
        );
    }
});

var QuickValuesVisualization = React.createClass({
    getInitialState() {
        return {
            total: undefined,
            others: undefined,
            missing: undefined,
            terms: {}
        };
    },
    componentWillReceiveProps(newProps) {
        var quickValues = newProps.data;
        this.setState({
            total: quickValues.total,
            others: quickValues.other,
            missing: quickValues.missing,
            terms: quickValues.terms
        });
    },
    _formatPercentage(percentage) {
        try {
            return numeral(percentage).format("0.00%");
        } catch (e) {
            return percentage;
        }
    },
    _formatCount(count) {
        try {
            return numeral(count).format("0,0");
        } catch(e) {
            return count;
        }
    },
    _getTableData() {
        var rows = [];
        var values = Object.keys(this.state.terms);

        if (values.length === 0) {
            return [];
        }

        values.forEach((term) => {
            var count = this.state.terms[term];
            var percentage = count / this.state.total;
            rows.push(
                <TableRowWrapper key={term}
                                 term={term}
                                 percentage={this._formatPercentage(percentage)}
                                 count={this._formatCount(count)}/>
            );
        });

        return rows;
    },
    render() {
        var tableData = this._getTableData();
        return (
            <div className="quickvalues-visualization">
                <table className="table table-condensed table-hover table-striped">
                    <thead>
                        <tr>
                            <th style={{width: "225px"}}>Value</th>
                            <th style={{width: "50px"}}>%</th>
                            <th>Count</th>
                        </tr>
                    </thead>

                    <tbody>
                        {tableData}
                    </tbody>
                </table>
            </div>
        );
    }
});

module.exports = QuickValuesVisualization;