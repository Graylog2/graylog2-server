'use strict';

var React = require('react');

var numeral = require('numeral');
var crossfilter = require('crossfilter');
var dc = require('dc');
var d3 = require('d3');

var QuickValuesVisualization = React.createClass({
    NUMBER_OF_TOP_VALUES: 5,
    getInitialState() {
        this.quickValuesData = crossfilter();
        this.dimension = this.quickValuesData.dimension((d) => d.count);

        return {
            total: undefined,
            others: undefined,
            missing: undefined,
            terms: {}
        };
    },
    componentDidMount() {
        var tableDomNode = this.getDOMNode().getElementsByTagName("table")[0];

        this.dataTable = dc.dataTable(tableDomNode);
        this.dataTable
            .dimension(this.dimension)
            .group((d) => {
                var topValues = this.dimension.top(this.NUMBER_OF_TOP_VALUES);
                var dInTopValues = topValues.some((value) => d.term.localeCompare(value.term) === 0);
                return dInTopValues ? "Top values" : "Others";
            })
            .size(50)
            .columns([
                (d) => d.term,
                (d) => {
                    var total = this.state.total - this.state.missing;
                    return this._formatPercentage(d.count / total);
                },
                (d) => this._formatCount(d.count)
            ])
            .sortBy((d) => d.count)
            .order(d3.descending)
            .on('renderlet', (table) => {
                table.selectAll(".dc-table-group").classed("info", true);
            });

        this.dataTable.render();
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
        } catch (e) {
            return count;
        }
    },
    componentWillReceiveProps(newProps) {
        var quickValues = newProps.data;

        var terms = Object.keys(quickValues.terms);

        var formattedTerms = terms.map((term) => {
            var count = quickValues.terms[term];
            return {
                term: term,
                count: count
            };
        });

        this.setState({
            total: quickValues.total,
            others: quickValues.other,
            missing: quickValues.missing,
            terms: formattedTerms
        }, this.drawData);
    },
    drawData() {
        this.quickValuesData.remove();
        this.quickValuesData.add(this.state.terms);
        this.dataTable.redraw();
    },
    render() {
        return (
            <div id={"visualization-" + this.props.id} className="quickvalues-visualization">
                <table className="table table-condensed table-striped table-hover">
                    <thead>
                    <tr>
                        <th style={{width: "225px"}}>Value</th>
                        <th style={{width: "50px"}}>%</th>
                        <th>Count</th>
                    </tr>
                    </thead>
                </table>
            </div>
        );
    }
});

module.exports = QuickValuesVisualization;