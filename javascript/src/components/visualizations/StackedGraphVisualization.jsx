/* global graphHelper, momentHelper */

'use strict';

var React = require('react');
var Immutable = require('immutable');
var numeral = require('numeral');
var c3 = require('c3');
var d3 = require('d3');

var D3Utils = require('../../util/D3Utils');
var NumberUtils = require('../../util/NumberUtils');

var StackedGraphVisualization = React.createClass({
    getInitialState() {
        this.normalizedData = false;
        this.series = Immutable.List();

        return {
            valueType: undefined,
            interpolation: undefined,
            dataPoints: Immutable.Set()
        };
    },
    componentDidMount() {
        this.renderGraph();
        this.setState({dataPoints: this._formatData(this.props.data)}, this.drawData);

    },
    componentWillReceiveProps(nextProps) {
        this.normalizedData = false;
        if (nextProps.height !== this.props.height || nextProps.width !== this.props.width) {
            this._resizeVisualization(nextProps.width, nextProps.height);
        }
        this.setState({dataPoints: this._formatData(nextProps.data)}, this.drawData);
    },
    _normalizeData(data) {
        if (this.normalizedData || data === null || data === undefined || !Array.isArray(data)) {
            return;
        }
        this.normalizedData = true;

        return data.map((dataPoint) => {
            dataPoint.y = NumberUtils.normalizeGraphNumber(dataPoint.y);
            return dataPoint;
        });
    },
    _formatData(data) {
        var normalizedData = this._normalizeData(data);
        var series = Immutable.Map();

        normalizedData.forEach((dataPoint) => {
            var timestamp = dataPoint.x * 1000;
            var formattedDataPoint = Immutable.Map({ timestamp: timestamp }).set("series" + dataPoint.series, dataPoint.y);
            if (series.has(timestamp)) {
                series = series.set(timestamp, series.get(timestamp).merge(formattedDataPoint));
            } else {
                series = series.set(timestamp, formattedDataPoint);
            }
        }, this);

        return series.toOrderedSet().sortBy((dataPoint) => dataPoint.get('timestamp'));
    },
    _getGraphType() {
        return (this.props.config.renderer === 'scatterplot') ? 'scatter' : this.props.config.renderer;
    },
    renderGraph() {
        var graphDomNode = React.findDOMNode(this);
        var colourPalette = D3Utils.glColourPalette();

        var i = 0;
        var names = Immutable.Map();
        var colours = Immutable.Map();

        this.props.config.series.forEach((seriesConfig) => {
            i++;
            var seriesName = "series" + i;
            this.series = this.series.push(seriesName);
            names = names.set(seriesName, seriesConfig['statistical_function'] + " " + seriesConfig['field'] + ", \"" + seriesConfig['query'] + "\"");
            colours = colours.set(seriesName, colourPalette(seriesName));
        });

        this.yAxisFormatter = (value) => {
            return Math.abs(value) > 1e+30 || value === 0 ? value.toPrecision(1) : d3.format(".2s")(value);
        };

        this.graph = c3.generate({
            bindto: graphDomNode,
            size: {
                height: this.props.height,
                width: this.props.width
            },
            type: 'bar',
            data: {
                columns: [],
                names: names.toJS(),
                colors: colours.toJS()
            },
            axis: {
                x: {
                    type: 'timeseries',
                    label: {
                        text: 'Time',
                        position: 'outer-center'
                    },
                    tick: {
                        format: graphHelper.customDateTimeFormat()
                    }
                },
                y: {
                    label: {
                        text: 'Values',
                        position: 'outer-middle'
                    },
                    tick: {
                        count: 3,
                        format: this.yAxisFormatter
                    }
                }
            },
            grid: {
                y: {
                    show: true,
                    ticks: 3
                },
                focus: {
                    show: false
                }
            },
            tooltip: {
                format: {
                    title: this._formatTooltipTitle,
                    value: this._formatTooltipValue
                }
            }
        });
    },
    _formatTooltipTitle(x) {
        return momentHelper.toUserTimeZone(x).format(momentHelper.HUMAN_TZ);
    },
    _formatTooltipValue(value, ratio, id, index) {
        var formattedValue;
        try {
            formattedValue = numeral(value).format("0,0.[00]");
        } catch (e) {
            formattedValue = d3.format(".2r")(value);
        }

        return formattedValue;
    },
    _resizeVisualization(width, height) {
        this.graph
            .width(width)
            .height(height);
        this.triggerRender = true;
    },
    drawData() {
        // Generate custom tick values for the time axis
        this.graph.internal.config.axis_x_tick_values = graphHelper.customTickInterval()(
            this.state.dataPoints.first().get('timestamp') - 1000,
            this.state.dataPoints.last().get('timestamp') + 1000
        );

        this.graph.load({
            json: this.state.dataPoints.toJS(),
            keys: {
                x: 'timestamp',
                value: this.series.toJS()
            },
            type: this._getGraphType()
        });
    },
    render() {
        return (
            <div id={"visualization-" + this.props.id} className={"graph " + this.props.config.renderer}/>
        );
    }
});

module.exports = StackedGraphVisualization;