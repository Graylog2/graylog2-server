import jQuery from 'jquery'; // We need to use this jQuery to send custom events
import moment from 'moment';
import numeral from 'numeral';
import Rickshaw from 'rickshaw';
import Qs from 'qs';
import naturalSort from 'javascript-natural-sort';

import momentHelper from 'legacy/moment-helper';
import rickshawHelper from 'legacy/rickshaw-helper';
import GraphVisualization from 'components/visualizations/GraphVisualization';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import jsRoutes from 'routing/jsRoutes';
import UserNotification from 'util/UserNotification';
import StringUtils from 'util/StringUtils';

// We need to require the other jQuery to make jQuery UI work :/
require('!script!../../../public/javascripts/jquery-2.1.1.min.js');
require('!script!../../../public/javascripts/jquery-ui.min.js');

function generateShortId() {
    return Math.random().toString(36).substr(2, 9);
}

export function generateId() {
    var r = "";
    for(var i = 0; i < 4; i++) {
        r = r + generateShortId();
    }

    return r;
}

export const FieldChart = {
    fieldGraphs: {},
    GRAPH_HEIGHT: 120,
    palette: new Rickshaw.Color.Palette({scheme: 'colorwheel'}),

    reload() {
        this.palette = new Rickshaw.Color.Palette({scheme: 'colorwheel'});
    },

    _getDefaultOptions(opts) {
        var searchParams = {};
        jQuery(document).trigger("get-original-search.graylog.search", {
            callback(params) {
                searchParams = params.toJS();
            }
        });

        // Options.
        if (opts.chartid == undefined) {
            opts.chartid = generateId();
        }

        if (opts.interval == undefined) {
            opts.interval = searchParams['interval'] || 'minute';
        }

        if (opts.interpolation == undefined) {
            opts.interpolation = "linear";
        }

        if (opts.renderer == undefined) {
            opts.renderer = "bar";
        }

        if (opts.valuetype == undefined) {
            opts.valuetype = "mean";
        }

        if (opts.streamid == undefined) {
            opts.streamid = "";
        }

        if (opts.query == undefined) {
            opts.query = searchParams['query'];
        }

        if (opts.rangetype == undefined) {
            opts.rangetype = searchParams['range_type'];
        }

        if (opts.range == undefined) {
            opts.range = {};
        }

        if (opts.createdAt === undefined) {
            opts.createdAt = moment().valueOf();
        }

        switch (opts.rangetype) {
            case "relative":
                if (opts.range.relative == undefined) {
                    opts.range.relative = searchParams['relative'];
                }
                break;
            case "absolute":
                if (opts.range.from == undefined) {
                    opts.range.from = searchParams['from'];
                }

                if (opts.range.to == undefined) {
                    opts.range.to = searchParams['to'];
                }
                break;
            case "keyword":
                if (opts.range.keyword == undefined) {
                    opts.range.keyword = searchParams['keyword'];
                }
                break;
        }

        return opts;
    },

    _getTimeRangeParams(opts) {
        var timerange = {};
        switch (opts.rangetype) {
            case "relative":
                timerange["range"] = opts.range.relative;
                break;
            case "absolute":
                timerange["from"] = opts.range.from;
                timerange["to"] = opts.range.to;
                break;
            case "keyword":
                timerange["keyword"] = opts.range.keyword;
                break;
        }

        return timerange;
    },

    _onFieldHistogramLoad($graphContainer, $graphElement, $graphYAxis, opts, data) {
        if (opts.query.trim().length > 0) {
            $(".field-graph-query", $graphContainer).text(opts.query);
        } else {
            $(".field-graph-query", $graphContainer).text("*");
        }

        var lines = [];
        lines.push(JSON.stringify(opts));
        $graphContainer.attr("data-lines", lines);

        $(".type-description", $graphContainer).text("[" + GraphVisualization.getReadableFieldChartStatisticalFunction(opts.valuetype) + "] " + opts.field + ", ");

        // Do not add from time when we search in all messages
        var from = $graphContainer.data('from') !== undefined ? data.from : undefined;

        rickshawHelper.processHistogramData(data.values, from, data.to, data.interval);

        var graph = new Rickshaw.Graph({
            element: $graphElement[0],
            width: $graphElement.width(),
            height: this.GRAPH_HEIGHT,
            interpolation: opts.interpolation,
            renderer: rickshawHelper.getRenderer(opts.renderer),
            resolution: data.interval,
            series: [{
                name: "value",
                data: data.values,
                color: '#26ADE4',
                gl2_query: opts.query,
                valuetype: GraphVisualization.getReadableFieldChartStatisticalFunction(opts.valuetype),
                field: opts.field
            }]
        });

        new Rickshaw.Graph.Axis.Y({
            graph: graph,
            tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
            orientation: 'left',
            element: $graphYAxis[0],
            pixelsPerTick: 30
        });

        new Rickshaw.Graph.Axis.Time({
            graph: graph,
            ticksTreatment: "glow",
            timeFixture: new Rickshaw.Fixtures.Graylog2Time() // Cares about correct TZ handling.
        });

        new Rickshaw.Graph.HoverDetail({
            graph: graph,
            formatter: function (series, x, y) {
                var dateMoment = momentHelper.toUserTimeZone(new Date(x * 1000));
                var date = '<span class="date">' + dateMoment.format('ddd MMM DD YYYY HH:mm:ss ZZ') + '</span>';
                var swatch = '<span class="detail_swatch" style="background-color: ' + series.color + '"></span>';
                var content = swatch + '[' + series.valuetype + '] ' + series.field + ': ' + numeral(y).format('0,0.[000]') + '<br>' + date;
                return content;
            }
        });

        new Rickshaw.Graph.Graylog2Selector({
            graph: graph
        });

        if (opts.renderer == "scatterplot") {
            graph.renderer.dotSize = 2;
        }

        if (opts.renderer == "area") {
            graph.renderer.stroke = true;
        }

        graph.render();

        this.fieldGraphs[opts.chartid] = graph;

        // Make it draggable.
        $graphContainer.draggable({
            handle: ".reposition-handle",
            cursor: "move",
            scope: "#field-graphs",
            revert: "invalid", // return to position when not dropped on a droppable.
            opacity: 0.5,
            containment: $("#field-graphs"),
            axis: "y",
            snap: $(".field-graph-container"),
            snapMode: "inner"
        });

        const that = this;

        // ...and droppable.
        $graphContainer.droppable({
            scope: "#field-graphs",
            tolerance: "intersect",
            activate(event, ui) {
                // Show merge hints on all charts except the dragged one when dragging starts.
                $("#field-graphs .merge-hint").not($(".merge-hint", ui.draggable)).show();
            },
            deactivate() {
                // Hide all merge hints when dragging stops.
                $("#field-graphs .merge-hint").hide();
            },
            over() {
                $(this).css("background-color", "#C7E2ED");
                $(".merge-hint span", $(this)).switchClass("alpha80", "merge-drop-ready");
            },
            out() {
                $(this).css("background-color", "#fff");
                $(".merge-hint span", $(this)).switchClass("merge-drop-ready", "alpha80");
            },
            drop(event, ui) {
                // Merge charts.
                var target = $(this).attr("data-chart-id");
                var dragged = ui.draggable.attr("data-chart-id");

                ui.draggable.hide();
                $(this).css("background-color", "#fff");

                that._mergeCharts(target, dragged);
            },
        });
    },

    _onFieldHistogramFail($graphElement, opts, error) {
        if (error.status === 400) {
            sendFailureEvent(opts.chartid, "Field graphs are only available for numeric fields.");
        } else {
            var alert = $('<div>').addClass('alert').addClass('alert-warning').text('Field graph could not be loaded, please try again after reloading the page.');
            $graphElement.append(alert);
            UserNotification.error("Loading field graph for '" + opts.field + "' failed with status: " + error.status);
            console.error(error);
        }
    },

    _chartOptionsFromContainer(cc) {
        try {
            return JSON.parse(cc.attr("data-lines"));
        } catch (e) {
            return this._getDefaultOptions();
        }
    },

    _changeGraphConfig(graphContainer, key, value) {
        const options = this._chartOptionsFromContainer(graphContainer);
        options[key] = value;

        graphContainer.attr("data-lines", JSON.stringify(options));
        sendUpdatedGraphEvent(options);
    },

    _insertSpinner($graphContainer) {
        var spinnerElement = $(`<div class="spinner" style="height: ${this.GRAPH_HEIGHT}px; line-height: ${this.GRAPH_HEIGHT}px;"><i class="fa fa-spin fa-refresh fa-3x spinner"></i></div>`);
        $graphContainer.append(spinnerElement);
    },

    _deleteSpinner($graphContainer) {
        $(".spinner", $graphContainer).remove();
    },

    renderFieldChart(opts, graphContainer, renderingOptions) {
        var field = opts.field;
        var $graphContainer = $(graphContainer);

        this._insertSpinner($graphContainer);

        // Delete a possibly already existing graph to manage updates.
        var $graphElement = $('.field-graph', $graphContainer);
        var $graphYAxis = $('.field-graph-y-axis', $graphContainer);
        $graphElement.html("");
        $graphYAxis.html("");
        $graphYAxis.hide();

        opts = this._getDefaultOptions(opts);
        const timeRangeParams = this._getTimeRangeParams(opts);

        const url = jsRoutes.controllers.api.UniversalSearchApiController.fieldHistogram(
          opts.rangetype,
          opts.query || '*',
          opts.field,
          opts.interval,
          Qs.stringify(timeRangeParams)
          //opts.streamid,
        ).url;

        const promise = fetch('GET', URLUtils.qualifyUrl(url))
          .then(request => {
              const formattedRequest = {
                  time: request.time,
                  interval: request.interval,
                  from: request.queried_timerange.from,
                  to: request.queried_timerange.to,
                  values: [],
              };

              Object.keys(request.results)
                  .sort(naturalSort)
                  .map(timestamp => {
                      formattedRequest.values.push({x: Number(timestamp), y: request.results[timestamp][opts.valuetype]});
                  });

              return formattedRequest;
          });

        promise
          .then(data => {
              this._onFieldHistogramLoad($graphContainer, $graphElement, $graphYAxis, opts, data);

              if (renderingOptions && renderingOptions.newGraph) {
                  sendCreatedGraphEvent(opts);
              }
          })
          .catch(error => this._onFieldHistogramFail($graphElement, opts, error))
          .finally(() => {
              $graphYAxis.show();
              this._deleteSpinner($graphContainer);
          });
    },

    renderNewFieldChart(options, graphContainer) {
        this.renderFieldChart(options, graphContainer, {newGraph: true});
    },

    changeInterpolation(graphContainer, interpolation) {
        const graphOptions = this._chartOptionsFromContainer(graphContainer);
        this._changeGraphConfig(graphContainer, "interpolation", interpolation);

        const graph = this.fieldGraphs[graphOptions.chartid];
        graph.interpolation = interpolation;
        graph.render();
    },

    changeRenderer(graphContainer, renderer) {
        const graphOptions = this._chartOptionsFromContainer(graphContainer);
        this._changeGraphConfig(graphContainer, "renderer", renderer);

        const graph = this.fieldGraphs[graphOptions.chartid];
        graph.setRenderer(renderer);

        if (renderer == "scatterplot") {
            graph.renderer.dotSize = 2;
        }

        if (renderer == "area") {
            graph.renderer.stroke = true;
        }

        graph.renderer.unstack = true;
        graph.render();
    },

    changeResolution(graphContainer, resolution) {
        const graphOptions = this._chartOptionsFromContainer(graphContainer);
        graphOptions.interval = resolution;
        this._changeGraphConfig(graphContainer, "interval", resolution);
        this.renderFieldChart(graphOptions, graphContainer);
    },

    changeStatisticalFunction(graphContainer, statisticalFunction) {
        var graphOptions = this._chartOptionsFromContainer(graphContainer);
        graphOptions.valuetype = statisticalFunction;
        this._changeGraphConfig(graphContainer, "valuetype", statisticalFunction);
        this.renderFieldChart(graphOptions, graphContainer);
    },

    _mergeCharts(targetId, draggedId) {
        var targetChart = this.fieldGraphs[targetId];
        var draggedChart = this.fieldGraphs[draggedId];

        var targetElem = $('.field-graph-container[data-chart-id="' + targetId + '"]');

        for (var i = 0; i < draggedChart.series.length; i++) {
            var lineColor = this.palette.color();
            var series = draggedChart.series[i];
            var query = series.gl2_query;

            if (query == undefined || query == "") {
                query = "*";
            }

            // Add query to query list of chart.
            var queryDescription = "<div class='field-graph-query-color' style='background-color: " + lineColor + ";'></div> "
              + "<span class=\"type-description\">[" + StringUtils.escapeHTML(series.valuetype) + "] " + series.field + ", </span> "
              + "Query: <span class='field-graph-query'>" + StringUtils.escapeHTML(query) + "</span>";

            $("ul.field-graph-query-container", targetElem).append("<li>" + queryDescription + "</li>");

            var addSeries = {
                name: "value" + i,
                color: lineColor,
                gl2_query: query,
                valuetype: GraphVisualization.getReadableFieldChartStatisticalFunction(series.valuetype),
                field: series.field
            };

            addSeries["data"] = series.data;

            targetChart.series.push(addSeries);
        }

        targetChart.renderer.unstack = true;

        sendMergedGraphsEvent(targetId, draggedId);

        // Reflect all the chart changes we made.
        targetChart.update();
        targetChart.render();
    },

    stackGraphs(targetGraphId, sourceGraphId) {
        this._mergeCharts(targetGraphId, sourceGraphId);
        const sourceGraphElement = $('.field-graph-container[data-chart-id="' + sourceGraphId + '"]');
        sourceGraphElement.hide();
    },
};


function sendCreatedGraphEvent(opts) {
    jQuery(document).trigger('created.graylog.fieldgraph', {graphOptions: opts});
}

function sendUpdatedGraphEvent(opts) {
    jQuery(document).trigger('updated.graylog.fieldgraph', {graphOptions: opts});
}

function sendFailureEvent(graphId, errorMessage) {
    jQuery(document).trigger('failed.graylog.fieldgraph', {graphId: graphId, errorMessage: errorMessage});
}

function sendMergedGraphsEvent(targetGraphId, draggedGraphId) {
    jQuery(document).trigger('merged.graylog.fieldgraph', {targetGraphId: targetGraphId, draggedGraphId: draggedGraphId});
}

// Changing type of value graphs.
$(document).on("click", ".field-graph-container ul.renderer-selector li a", function (e) {
    e.preventDefault();

    var graphContainer = $(this).closest(".field-graph-container");
    var type = $(this).attr("data-type");
    FieldChart.changeRenderer(graphContainer, type);
});

// Changing interpolation of value graphs.
$(document).on("click", ".field-graph-container ul.interpolation-selector li a", function (e) {
    e.preventDefault();

    var graphContainer = $(this).closest(".field-graph-container");
    var interpolation = $(this).attr('data-type');
    FieldChart.changeInterpolation(graphContainer, interpolation);
});

// Changing interval of value graphs.
$(document).on("click", ".field-graph-container ul.interval-selector li a", function (e) {
    e.preventDefault();

    var graphContainer = $(this).closest(".field-graph-container");
    var interval = $(this).attr("data-type");
    FieldChart.changeResolution(graphContainer, interval);
});

// Changing value type of value graphs.
$(document).on("click", ".field-graph-container ul.valuetype-selector li a", function (e) {
    e.preventDefault();

    var graphContainer = $(this).closest(".field-graph-container");
    var valuetype = $(this).attr("data-type");
    FieldChart.changeStatisticalFunction(graphContainer, valuetype);
});
