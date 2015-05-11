'use strict';

var React = require('react');

var numeral = require('numeral');
var crossfilter = require('crossfilter');
var dc = require('dc');
var d3 = require('d3');
var $ = require('jquery');

var D3Utils = require('../../util/D3Utils');

var QuickValuesVisualization = React.createClass({
    NUMBER_OF_TOP_VALUES: 5,
    getInitialState() {
        this.dcGroupName = "quickvalue-" + this.props.id;
        this.quickValuesData = crossfilter();
        this.dimension = this.quickValuesData.dimension((d) => d.term);
        this.group = this.dimension.group().reduceSum((d) => d.count);

        return {
            total: undefined,
            others: undefined,
            missing: undefined,
            terms: {}
        };
    },
    componentDidMount() {
        this._renderDataTable();
        this._renderPieChart();
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
    _renderDataTable() {
        var tableDomNode = this.refs.table.getDOMNode();

        this.dataTable = dc.dataTable(tableDomNode, this.dcGroupName);
        this.dataTable
            .dimension(this.dimension)
            .group((d) => {
                var topValues = this.group.top(this.NUMBER_OF_TOP_VALUES);
                var dInTopValues = topValues.some((value) => d.term.localeCompare(value.key) === 0);
                return dInTopValues ? "Top values" : "Others";
            })
            .size(50)
            .columns([
                (d) => {
                    var colourBadge = "";

                    if (typeof this.pieChart !== 'undefined' && this.dataTable.group()(d) !== 'Others') {
                        var colour = this.pieChart.colors()(d.term);
                        colourBadge = "<span class=\"datatable-badge\" style=\"background-color: " + colour + "\"></span>";
                    }

                    return colourBadge + " " + d.term;
                },
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
    _renderPieChart() {
        var graphDomNode = this.refs.graph.getDOMNode();

        this.pieChart = dc.pieChart(graphDomNode, this.dcGroupName);
        this.pieChart
            .dimension(this.dimension)
            .group(this.group)
            .height(200)
            .width(200)
            .renderLabel(false)
            .renderTitle(false)
            .slicesCap(this.NUMBER_OF_TOP_VALUES)
            .ordering((d) => d.value)
            .colors(D3Utils.glColourPalette());

        D3Utils.tooltipRenderlet(this.pieChart, 'g.pie-slice', this._formatGraphTooltip);

        $(graphDomNode).tooltip({
            'selector': '[rel="tooltip"]',
            'container': 'body',
            'placement': 'auto',
            'delay': {show: 300, hide: 100},
            'html': true
        });

        this.pieChart.render();
    },
    _formatGraphTooltip(d) {
        var valueText = d.data.key + ": " + this._formatCount(d.value) + "<br>";

        return "<div class=\"datapoint-info\">" + valueText + "</div>";
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
    drawData() {
        this.quickValuesData.remove();
        this.quickValuesData.add(this.state.terms);
        this.dataTable.redraw();

        if (this.props.config.show_pie_chart) {
            this.pieChart.redraw();
        }
    },
    render() {
        var pieChartClassName;

        if (this.props.config.show_pie_chart) {
            pieChartClassName = this.props.horizontal ? 'col-md-4' : 'col-md-12';
        } else {
            pieChartClassName = 'hidden';
        }

        var dataTableClassName;

        if (this.props.config.show_data_table) {
            dataTableClassName = this.props.horizontal ? 'col-md-8' : 'col-md-12';
        } else {
            dataTableClassName = 'hidden';
        }

        return (
            <div id={"visualization-" + this.props.id} className="quickvalues-visualization">
                <div className="container-fluid">
                    <div className="row">
                        <div className={pieChartClassName}>
                            <div ref="graph" className="quickvalues-graph"/>
                        </div>
                        <div className={dataTableClassName}>
                            <div className="quickvalues-table">
                                <table ref="table" className="table table-condensed table-striped table-hover">
                                    <thead>
                                    <tr>
                                        <th style={{width: "225px"}}>Value</th>
                                        <th style={{width: "50px"}}>%</th>
                                        <th>Count</th>
                                    </tr>
                                    </thead>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
});

module.exports = QuickValuesVisualization;