/* global resultHistogram */

'use strict';

var React = require('react');

// Hue-manatee. We tried to be sorry, but aren't.
var LegacyHistogram = React.createClass({
    RESOLUTIONS: ['year', 'quarter', 'month', 'week', 'day', 'hour', 'minute'],
    componentDidMount() {
        resultHistogram.resetContainerElements(React.findDOMNode(this));
        resultHistogram.setData(this.props.formattedHistogram);
        resultHistogram.drawResultGraph();
    },
    render() {
        var resolutionLinks = this.RESOLUTIONS.map((resolution) => {
            var className = "date-histogram-res-selector";
            if (this.props.histogram.interval === resolution) {
                className += " selected-resolution";
            }
            var suffix = resolution === this.RESOLUTIONS[this.RESOLUTIONS.length - 1] ? "" : ",";
            return (
                <li key={resolution}>
                    <a href="/searchv2" className={className} data-resolution={resolution}>{resolution}</a>
                    {suffix}
                </li>
            );
        });

        var resolutionSelector = (
            <ul className="graph-resolution-selector list-inline">
                <li><i className="fa fa-clock-o"></i></li>
                {resolutionLinks}
            </ul>
        );

        return (<div className="content-col">
            <div className="pull-right">TODO dashboards</div>
            <h1>Histogram</h1>

            {resolutionSelector}

            <div id="result-graph-container">
                <div id="y_axis"></div>
                <div id="result-graph" data-from={this.props.histogram['histogram_boundaries'].from}
                     data-to={this.props.histogram['histogram_boundaries'].to}></div>
                <div id="result-graph-timeline"></div>
            </div>

        </div>);
    }
});

module.exports = LegacyHistogram;