/* global resultHistogram */

'use strict';

var React = require('react');

// Hue-manatee. We tried to be sorry, but aren't.
var LegacyHistogram = React.createClass({
    componentDidMount() {
        resultHistogram.resetContainerElements(React.findDOMNode(this));
        resultHistogram.setData(this.props.formattedHistogram);
        resultHistogram.drawResultGraph();
    },
    render() {
        return (<div className="content-col">
            <div className="pull-right">TODO dashboards</div>
            <h1>Histogram</h1>

            <div className="graph-resolution-selector">
                <i className="fa fa-time"></i>
                TODO selector
            </div>
            <div id="result-graph-container">
                <div id="y_axis"></div>
                <div id="result-graph" data-from={this.props.histogram['histogram_boundaries'].from} data-to={this.props.histogram['histogram_boundaries'].to}></div>
                <div id="result-graph-timeline"></div>
            </div>

        </div>);
    }
});

module.exports = LegacyHistogram;