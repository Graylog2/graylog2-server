/* global graphHelper */
/* jshint -W107 */

'use strict';

var React = require('react');

var $ = require('jquery');

var dc = require('dc');
var d3 = require('d3');

var UniversalSearch = require('../../logic/search/UniversalSearch');

var SourceTitle = require('./SourceTitle');
var D3Utils = require('../../util/D3Utils');

var SourceLineChart = React.createClass({
    getInitialState() {
        return {
            lineChartWidth: "100%"
        };
    },
    renderLineChart(dimension, group, onDataFiltered) {
        var lineChartDomNode = $("#dc-sources-line-chart")[0];
        var width = $(lineChartDomNode).width();
        $(document).on('mouseup', "#dc-sources-line-chart svg", (event) => {
            $(".timerange-selector-container").effect("bounce", {
                complete: () => {
                    // Submit search directly if alt key is pressed.
                    if (event.altKey) {
                        UniversalSearch.submit();
                    }
                }
            });
        });
        this._lineChart = dc.lineChart(lineChartDomNode);
        this._lineChart
            .height(200)
            .margins({left: 35, right: 20, top: 20, bottom: 20})
            .dimension(dimension)
            .group(group)
            .x(d3.time.scale())
            .renderHorizontalGridLines(true)
            // FIXME: causes those nasty exceptions when rendering data (one per x axis tick)
            .elasticX(true)
            .elasticY(true)
            .transitionDuration(30)
            .colors(D3Utils.glColourPalette())
            .on("filtered", (chart) => {
                dc.events.trigger(() => {
                    var filter = chart.filter();
                    onDataFiltered(filter);
                });
            });
        this._configureWidth(width);
        this._lineChart.xAxis()
            .ticks(graphHelper.customTickInterval())
            .tickFormat(graphHelper.customDateTimeFormat());
        this._lineChart.yAxis()
            .ticks(6)
            .tickFormat(d3.format("s"));
    },
    clearFilters() {
        this._lineChart.filterAll();
    },
    getFilters() {
        return this._lineChart ? this._lineChart.filters() : [];
    },
    setFilter(filter) {
        this._lineChart.filter(filter);
    },
    _configureWidth(lineChartWidth) {
        this._lineChart
            .width(lineChartWidth);
        this.setState({lineChartWidth: String(lineChartWidth) + "px"});
    },
    updateWidth() {
        var lineChartDomNode = $("#dc-sources-line-chart");
        var lineChartWidth = lineChartDomNode.width();
        this._configureWidth(lineChartWidth);
    },
    render() {
        var loadingSpinnerStyle = {
            display: this.props.reloadingHistogram ? 'block' : 'none',
            width: this.state.lineChartWidth
        };
        var loadingSpinner = (
            <div className="sources overlay" style={loadingSpinnerStyle}>
                <i className="fa fa-spin fa-refresh spinner"></i>
            </div>
        );

        var noDataOverlayStyle = {
            display: this.props.histogramDataAvailable ? 'none' : 'block',
            width: this.state.lineChartWidth
        };
        var noDataOverlay = (
            <div className="sources overlay" style={noDataOverlayStyle}>Not enough data</div>
        );
        return (
            <div id="dc-sources-line-chart" className="col-md-12">
                <SourceTitle className="reset" resetFilters={this.props.resetFilters}>Messages per {this.props.resolution}</SourceTitle>
                {loadingSpinner}
                {noDataOverlay}
            </div>
        );

    }
});

module.exports = SourceLineChart;