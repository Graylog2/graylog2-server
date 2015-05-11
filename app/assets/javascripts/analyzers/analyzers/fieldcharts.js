$(document).ready(function () {
    var GRAPH_HEIGHT = 120;

    function insertSpinner($graphContainer) {
        var spinnerElement = $('<div class="spinner" style="height: ' + GRAPH_HEIGHT + 'px; line-height: ' + GRAPH_HEIGHT + 'px;"><i class="fa fa-spin fa-refresh fa-3x spinner"></i></div>');
        $graphContainer.append(spinnerElement);
    }

    function deleteSpinner($graphContainer) {
        $(".spinner", $graphContainer).remove();
    }

    var palette = new Rickshaw.Color.Palette({scheme: 'colorwheel'});

    $(document).on('create.graylog.fieldgraph', function (event, data) {
        "use strict";
        var graphOptions = createFieldChart(data['options'], data['container']);
        $(document).trigger('created.graylog.fieldgraph', {graphOptions: graphOptions});
    });

    function sendUpdateGraphEvent(opts) {
        "use strict";
        $(document).trigger('updated.graylog.fieldgraph', {graphOptions: opts});
    }

    function sendFailureEvent(graphId, errorMessage) {
        $(document).trigger('failed.graylog.fieldgraph', {graphId: graphId, errorMessage: errorMessage});
    }

    function createFieldChart(options, graphContainer) {
        "use strict";

        //var container = $(this).closest(".analyze-field");
        //if (!!container.attr("data-stream-id")) {
        //    opts.streamid = container.attr("data-stream-id");
        //}
        //
        //renderFieldChart(opts, container);

        renderFieldChart(options, graphContainer);

        return options;
    }

    function getDefaultOptions(opts) {
        var searchParams = {};
        $(document).trigger("get-original-search.graylog.search", {
            callback: function (params) {
                searchParams = params.toJS()
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
    }

    function getParams(opts) {
        "use strict";
        var params = {
            "rangetype": opts.rangetype,
            "q": opts.query,
            "field": opts.field,
            "interval": opts.interval,
            "valueType": opts.valuetype,
            "streamId": opts.streamid
        };

        switch (opts.rangetype) {
            case "relative":
                params["relative"] = opts.range.relative;
                break;
            case "absolute":
                params["from"] = opts.range.from;
                params["to"] = opts.range.to;
                break;
            case "keyword":
                params["keyword"] = opts.range.keyword;
                break;
        }

        return params;
    }

    function renderFieldChart(opts, graphContainer) {
        var field = opts.field;
        var $graphContainer = $(graphContainer);

        opts = getDefaultOptions(opts);
        var params = getParams(opts);

        insertSpinner($graphContainer);

        // Delete a possibly already existing graph to manage updates.
        $('.field-graph-y-axis', $graphContainer).html("");
        $('.field-graph', $graphContainer).html("");

        $.ajax({
            url: appPrefixed('/a/search/fieldhistogram'),
            data: params,
            success: function (data) {
                var $graphElement = $('.field-graph', $graphContainer);
                var $graphYAxis = $('.field-graph-y-axis', $graphContainer);

                $("ul", $graphContainer).attr("data-field", field);

                if (opts.query.trim().length > 0) {
                    $(".field-graph-query", $graphContainer).text(opts.query);
                } else {
                    $(".field-graph-query", $graphContainer).text("*");
                }

                var lines = [];
                lines.push(JSON.stringify(opts));
                $graphContainer.attr("data-lines", lines);

                $(".type-description", $graphContainer).text("[" + opts.valuetype + "] " + opts.field + ", ");

                rickshawHelper.processHistogramData(data.values, data.from, data.to, data.interval);

                var graph = new Rickshaw.Graph({
                    element: $graphElement[0],
                    width: $graphElement.width(),
                    height: GRAPH_HEIGHT,
                    interpolation: opts.interpolation,
                    renderer: rickshawHelper.getRenderer(opts.renderer),
                    resolution: data.interval,
                    series: [{
                        name: "value",
                        data: data.values,
                        color: '#26ADE4',
                        gl2_query: opts.query,
                        valuetype: opts.valuetype,
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

                // Highlight menu items.
                $("ul.interval-selector li a", $graphContainer).removeClass("selected");
                $('ul.interval-selector li a[data-type="' + opts.interval + '"]', $graphContainer).addClass("selected");

                $("ul.interpolation-selector li a", $graphContainer).removeClass("selected");
                $('ul.interpolation-selector li a[data-type="' + opts.interpolation + '"]', $graphContainer).addClass("selected");

                $("ul.type-selector li a", $graphContainer).removeClass("selected");
                $('ul.type-selector li a[data-type="' + opts.renderer + '"]', $graphContainer).addClass("selected");

                $("ul.valuetype-selector li a", $graphContainer).removeClass("selected");
                $('ul.valuetype-selector li a[data-type="' + opts.valuetype + '"]', $graphContainer).addClass("selected");

                fieldGraphs[opts.chartid] = graph;

                sendUpdateGraphEvent(opts);

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

                // ...and droppable.
                $graphContainer.droppable({
                    scope: "#field-graphs",
                    tolerance: "intersect",
                    activate: function (event, ui) {
                        // Show merge hints on all charts except the dragged one when dragging starts.
                        $("#field-graphs .merge-hint").not($(".merge-hint", ui.draggable)).show();
                    },
                    deactivate: function () {
                        // Hide all merge hints when dragging stops.
                        $("#field-graphs .merge-hint").hide();
                    },
                    over: function () {
                        $(this).css("background-color", "#C7E2ED");
                        $(".merge-hint span", $(this)).switchClass("alpha80", "merge-drop-ready");
                    },
                    out: function () {
                        $(this).css("background-color", "#fff");
                        $(".merge-hint span", $(this)).switchClass("merge-drop-ready", "alpha80");
                    },
                    drop: function (event, ui) {
                        // Merge charts.
                        var target = $(this).attr("data-chart-id");
                        var dragged = ui.draggable.attr("data-chart-id");

                        ui.draggable.hide();
                        $(this).css("background-color", "#fff");

                        mergeCharts(target, dragged);
                    }
                });
            },
            error: function (data) {
                if (data.status != 400) {
                    showError("Loading field graph for '" + opts.field + "' failed with status: " + data.status);
                }
            },
            statusCode: {
                400: function () {
                    sendFailureEvent(opts.chartid, "Field graphs are only available for numeric fields.");
                }
            },
            complete: function () {
                deleteSpinner($graphContainer);
            }
        });
    }

    // Changing type of value graphs.
    $(document).on("click", ".field-graph-container ul.type-selector li a", function (e) {
        e.preventDefault();

        var field = $(this).closest("ul").attr("data-field");
        var type = $(this).attr("data-type");

        var graphContainer = $(this).closest(".field-graph-container");
        var graphOpts = chartOptionsFromContainer(graphContainer);
        changeGraphConfig(graphContainer, "renderer", type);

        var graph = fieldGraphs[graphOpts.chartid];
        graph.setRenderer(type);

        if (type == "scatterplot") {
            graph.renderer.dotSize = 2;
        }

        if (type == "area") {
            graph.renderer.stroke = true;
        }

        graph.renderer.unstack = true;

        graph.render();

        sendUpdateGraphEvent(chartOptionsFromContainer(graphContainer));

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Changing interpolation of value graphs.
    $(document).on("click", ".field-graph-container ul.interpolation-selector li a", function (e) {
        e.preventDefault();

        var field = $(this).closest("ul").attr("data-field");
        var interpolation = $(this).text();

        var graphContainer = $(this).closest(".field-graph-container");
        var graphOpts = chartOptionsFromContainer(graphContainer);
        changeGraphConfig(graphContainer, "interpolation", interpolation);

        var graph = fieldGraphs[graphOpts.chartid];
        graph.interpolation = interpolation;
        graph.render();

        sendUpdateGraphEvent(chartOptionsFromContainer(graphContainer));

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Changing interval of value graphs.
    $(document).on("click", ".field-graph-container ul.interval-selector li a", function (e) {
        e.preventDefault();
        var field = $(this).closest("ul").attr("data-field");
        var graphContainer = $(this).closest(".field-graph-container");

        var interval = $(this).attr("data-type");
        var opts = chartOptionsFromContainer(graphContainer);
        opts.interval = interval;
        opts.field = field;

        renderFieldChart(opts, graphContainer);
        changeGraphConfig(graphContainer, "interval", interval);

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Changing value type of value graphs.
    $(document).on("click", ".field-graph-container ul.valuetype-selector li a", function (e) {
        e.preventDefault();
        var field = $(this).closest("ul").attr("data-field");
        var graphContainer = $(this).closest(".field-graph-container");

        var valuetype = $(this).attr("data-type");
        var opts = chartOptionsFromContainer(graphContainer);
        opts.valuetype = valuetype;
        opts.field = field;

        renderFieldChart(opts, graphContainer);
        changeGraphConfig(graphContainer, "valuetype", valuetype);

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    function chartOptionsFromContainer(cc) {
        return JSON.parse(cc.attr("data-lines"));
    }

    function changeGraphConfig(graphContainer, key, value) {
        var opts = chartOptionsFromContainer(graphContainer);
        opts[key] = value;

        graphContainer.attr("data-lines", JSON.stringify(opts));
    }

    function mergeCharts(targetId, draggedId) {
        var targetChart = fieldGraphs[targetId];
        var draggedChart = fieldGraphs[draggedId];

        var targetElem = $('.field-graph-container[data-chart-id="' + targetId + '"]');
        var draggedElem = $('.field-graph-container[data-chart-id="' + draggedId + '"]');

        var draggedOpts = JSON.parse(draggedElem.attr("data-lines"));

        // Update title and description.
        $("h1", targetElem).text("Combined graph");

        for (var i = 0; i < draggedChart.series.length; i++) {
            var lineColor = palette.color();
            var series = draggedChart.series[i];
            var query = series.gl2_query;

            if (query == undefined || query == "") {
                query = "*";
            }

            // Add query to query list of chart.
            var queryDescription = "<div class='field-graph-query-color' style='background-color: " + lineColor + ";'></div> "
                + "<span class=\"type-description\">[" + htmlEscape(series.valuetype) + "] " + series.field + ", </span> "
                + "Query: <span class='field-graph-query'>" + htmlEscape(query) + "</span>";

            $("ul.field-graph-query-container", targetElem).append("<li>" + queryDescription + "</li>");

            var addSeries = {
                name: "value" + i,
                color: lineColor,
                gl2_query: query,
                valuetype: series.valuetype,
                field: series.field
            };

            addSeries["data"] = series.data;

            targetChart.series.push(addSeries);
        }

        targetChart.renderer.unstack = true;

        $(".hide-combined-chart", targetElem).hide();

        // Reflect all the chart changes we made.
        targetChart.update();
        targetChart.render();
    }
});