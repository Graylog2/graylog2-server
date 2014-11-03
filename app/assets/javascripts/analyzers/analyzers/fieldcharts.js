$(document).ready(function() {
    var palette = new Rickshaw.Color.Palette({ scheme: 'colorwheel' });

    $(".analyze-field .line-chart").on("click", function(e) {
        e.preventDefault();

        opts = {};
        opts.field = $(this).attr("data-field");
        var container = $(this).closest(".analyze-field");
        if (!!container.attr("data-stream-id")) {
            opts.streamid = container.attr("data-stream-id");
        }

        renderFieldChart(opts);
    });

    function renderFieldChart(opts) {
        var field = opts.field;

        // Options.
        if (opts.chartid == undefined) {
            opts.chartid = generateId();
        }

        if (opts.interval == undefined) {
            opts.interval = $("#universalsearch-interval-permanent").text().trim();
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

        if (opts.pinned == undefined) {
            opts.pinned = false;
        }

        if (opts.query == undefined) {
            opts.query = $("#universalsearch-query-permanent").text().trim();
        }

        if (opts.rangetype == undefined) {
            opts.rangetype = $("#universalsearch-rangetype-permanent").text().trim();
        }

        if (opts.range == undefined) {
            opts.range = {};
        }

        var params = {
            "rangetype": opts.rangetype,
            "q": opts.query,
            "field": field,
            "interval": opts.interval,
            "valueType": opts.valuetype,
            "streamId": opts.streamid
        };

        switch(opts.rangetype) {
            case "relative":
                if (opts.range.relative == undefined) {
                    opts.range.relative = $("#universalsearch-relative-permanent").text();
                }

                params["relative"] = opts.range.relative;
                break;
            case "absolute":
                if (opts.range.from == undefined) {
                    opts.range.from = $("#universalsearch-from-permanent").text();
                }

                if (opts.range.to == undefined) {
                    opts.range.to = $("#universalsearch-to-permanent").text();
                }

                params["from"] = opts.range.from;
                params["to"] = opts.range.to;
                break;
            case "keyword":
                if (opts.range.keyword == undefined) {
                    opts.range.keyword = $("#universalsearch-keyword-permanent").text();
                }

                params["keyword"] = opts.range.keyword;
                break;
        }

        $("#field-graphs .spinner").show();

        /*
         * TODO:
         *   - export to image, ...
         *   - overflowing select box
         */

        // Delete a possibly already existing graph with this id. (for updates)
        $('.field-graph-container[data-chart-id="' + opts.chartid + '"]', $("#field-graphs")).remove();

        $.ajax({
            url: appPrefixed('/a/search/fieldhistogram'),
            data: params,
            success: function(data) {
                var template = $("#field-graph-template").clone();
                template.removeAttr("id");
                template.attr("data-chart-id", opts.chartid)
                template.attr("data-field", field);
                template.css("display", "block");
                $("h3 .title span", template).text(field);
                $("ul", template).attr("data-field", field);

                if (opts.query.trim().length > 0) {
                    $(".field-graph-query", template).text(opts.query);
                } else {
                    $(".field-graph-query", template).text("*");
                }

                var lines = [];
                lines.push(JSON.stringify(opts));
                template.attr("data-lines", lines);

                $(".type-description", template).text("[" + opts.valuetype + "] " + opts.field + ", ");

                if(opts.pinned) {
                    $(".pin", template).hide();
                    $(".unpin", template).show();
                }

                // Place the chart after all others but before the spinner.
                $("#field-graphs .spinner").before(template);

                var graphContainer = $('.field-graph-container[data-chart-id="' + opts.chartid + '"]', $("#field-graphs"));
                var graphElement = $('.field-graph', graphContainer);
                var graphYAxis = $('.field-graph-y-axis', graphContainer);

                var resultGraphElement = $("#result-graph");

                var graph = new Rickshaw.Graph( {
                    element: graphElement[0],
                    width: graphElement.width(),
                    height: 175,
                    interpolation: opts.interpolation,
                    renderer: rickshawHelper.getRenderer(opts.renderer),
                    resolution: data.interval,
                    series: [ {
                        name: "value",
                        data: rickshawHelper.correctDataBoundaries(data.values, resultGraphElement.data("from"), resultGraphElement.data("to"), data.interval),
                        color: '#26ADE4',
                        gl2_query: opts.query,
                        valuetype: opts.valuetype,
                        field: opts.field
                    } ]
                });

                new Rickshaw.Graph.Axis.Y( {
                    graph: graph,
                    tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
                    orientation: 'left',
                    element: graphYAxis[0],
                    pixelsPerTick: 30
                });

                new Rickshaw.Graph.Axis.Time({
                    graph: graph,
                    ticksTreatment: "glow",
                    timeFixture: new Rickshaw.Fixtures.Graylog2Time(gl2UserTimeZoneOffset) // Cares about correct TZ handling.
                });

                new Rickshaw.Graph.HoverDetail({
                    graph: graph,
                    formatter: function(series, x, y) {
                        var dateMoment = momentHelper.toUserTimeZone(new Date(x * 1000 ));
                        var date = '<span class="date">' + dateMoment.format('ddd MMM DD YYYY HH:mm:ss ZZ') + '</span>';
                        var swatch = '<span class="detail_swatch" style="background-color: ' + series.color + '"></span>';
                        var content = swatch + '[' + series.valuetype + '] ' + series.field + ': ' + numeral(y).format('0.[000]') + '<br>' + date;
                        return content;
                    }
                });

                new Rickshaw.Graph.Graylog2Selector( {
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
                $("ul.interval-selector li a", graphContainer).removeClass("selected");
                $('ul.interval-selector li a[data-type="' + opts.interval + '"]', graphContainer).addClass("selected");

                $("ul.interpolation-selector li a", graphContainer).removeClass("selected");
                $('ul.interpolation-selector li a[data-type="' + opts.interpolation + '"]', graphContainer).addClass("selected");

                $("ul.type-selector li a", graphContainer).removeClass("selected");
                $('ul.type-selector li a[data-type="' + opts.renderer + '"]', graphContainer).addClass("selected");

                $("ul.valuetype-selector li a", graphContainer).removeClass("selected");
                $('ul.valuetype-selector li a[data-type="' + opts.valuetype + '"]', graphContainer).addClass("selected");

                fieldGraphs[opts.chartid] = graph;

                // Is this chart pinned? We need to update it's settings then.
                var pinned = getPinnedCharts();
                if (pinned[opts.chartid] != undefined) {
                    pinned[opts.chartid] = opts;
                    setPinnedCharts(pinned);
                }

                // Make it draggable.
                graphContainer.draggable({
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
                graphContainer.droppable({
                    scope: "#field-graphs",
                    tolerance: "intersect",
                    activate: function(event, ui) {
                        // Show merge hints on all charts except the dragged one when dragging starts.
                        $("#field-graphs .merge-hint").not($(".merge-hint", ui.draggable)).show();
                    },
                    deactivate: function() {
                        // Hide all merge hints when dragging stops.
                        $("#field-graphs .merge-hint").hide();
                    },
                    over: function() {
                        $(this).css("background-color", "#C7E2ED");
                        $(".merge-hint span", $(this)).switchClass("alpha80", "merge-drop-ready");
                    },
                    out: function() {
                        $(this).css("background-color", "#fff");
                        $(".merge-hint span", $(this)).switchClass("merge-drop-ready", "alpha80");
                    },
                    drop: function(event, ui) {
                        // Merge charts.
                        var target = $(this).attr("data-chart-id");
                        var dragged = ui.draggable.attr("data-chart-id");

                        ui.draggable.hide();
                        $(this).css("background-color", "#fff");

                        mergeCharts(target, dragged);
                    }
                });
            },
            error: function(data) {
                if(data.status != 400) {
                    showError("Could not load histogram.");
                }
            },
            statusCode: { 400: function() {
                showError("Line charts are only available for numeric field types.");
            }},
            complete: function() {
                $("#field-graphs .spinner").hide();
            }
        });
    }

    // Changing type of value graphs.
    $(".field-graph-container ul.type-selector li a").live("click", function(e) {
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

        // Is this chart pinned? We need to update it's settings then.
        var pinned = getPinnedCharts();
        var chartId = graphOpts.chartid;
        if (pinned[chartId] != undefined) {
            pinned[chartId] = chartOptionsFromContainer(graphContainer);
            setPinnedCharts(pinned);
        }

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Changing interpolation of value graphs.
    $(".field-graph-container ul.interpolation-selector li a").live("click", function(e) {
        e.preventDefault();

        var field = $(this).closest("ul").attr("data-field");
        var interpolation = $(this).text();

        var graphContainer = $(this).closest(".field-graph-container");
        var graphOpts = chartOptionsFromContainer(graphContainer);
        changeGraphConfig(graphContainer, "interpolation", interpolation);

        var graph = fieldGraphs[graphOpts.chartid];
        graph.interpolation = interpolation;
        graph.render();

        // Is this chart pinned? We need to update it's settings then.
        var pinned = getPinnedCharts();
        var chartId = graphOpts.chartid;
        if (pinned[chartId] != undefined) {
            pinned[chartId] = chartOptionsFromContainer(graphContainer);
            setPinnedCharts(pinned);
        }

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Changing interval of value graphs.
    $(".field-graph-container ul.interval-selector li a").live("click", function(e) {
        e.preventDefault();
        var field = $(this).closest("ul").attr("data-field");
        var graphContainer = $(this).closest(".field-graph-container");

        var interval = $(this).attr("data-type");
        var opts = chartOptionsFromContainer(graphContainer);
        opts.interval = interval;
        opts.field = field;

        renderFieldChart(opts);
        changeGraphConfig(graphContainer, "interval", interval);

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Changing value type of value graphs.
    $(".field-graph-container ul.valuetype-selector li a").live("click", function(e) {
        e.preventDefault();
        var field = $(this).closest("ul").attr("data-field");
        var graphContainer = $(this).closest(".field-graph-container");

        var valuetype = $(this).attr("data-type");
        var opts = chartOptionsFromContainer(graphContainer);
        opts.valuetype = valuetype;
        opts.field = field;

        renderFieldChart(opts);
        changeGraphConfig(graphContainer, "valuetype", valuetype);

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Removing a value graph.
    $(".field-graph-container li a.hide").live("click", function(e) {
        e.preventDefault();
        var field = $(this).closest("ul").attr("data-field");
        var graphContainer = $(this).closest(".field-graph-container");
        var graphOpts = chartOptionsFromContainer(graphContainer);

        unpinChart(graphOpts.chartid);

        graphContainer.remove();
        delete fieldGraphs[graphOpts.chartid];
    });

    function chartOptionsFromContainer(cc) {
        return JSON.parse(cc.attr("data-lines"));
    }

    function changeGraphConfig(graphContainer, key, value) {
        var opts = chartOptionsFromContainer(graphContainer);
        opts[key] = value;

        graphContainer.attr("data-lines", JSON.stringify(opts));
    }

    $(".field-graph-container .pin").live("click", function(e) {
        e.preventDefault();
        var graphElem = $(this).closest(".field-graph-container");

        var chartOpts = chartOptionsFromContainer(graphElem);
        chartOpts.field = graphElem.attr("data-field");
        var chartId = chartOpts.chartid;

        // Get all pinned charts.
        var pinned = getPinnedCharts();

        // Write chart with current options.
        pinned[chartId] = chartOpts;

        setPinnedCharts(pinned);

        // Mark as pinned.
        $(this).hide();
        $(".unpin", graphElem).show();
    });

    $(".field-graph-container .unpin").live("click", function(e) {
        e.preventDefault();
        var graphElem = $(this).closest(".field-graph-container");
        var graphOpts = chartOptionsFromContainer(graphElem);

        changeGraphConfig(graphElem, "pinned", false);
        unpinChart(graphOpts.chartid);

        // Mark as unpinned.
        $(this).hide();
        $(".pin", graphElem).show();
    });

    $(".clear-pinned-charts").on("click", function(e) {
        e.preventDefault();

        setPinnedCharts({});

        // Mark all as unpinned.
        $(".field-graph-container .unpin").hide();
        $(".field-graph-container .pin").show();

        $(this).hide();
        showSuccess("All charts have been unpinned.")
    });

    function mergeCharts(targetId, draggedId) {
        var targetChart = fieldGraphs[targetId];
        var draggedChart = fieldGraphs[draggedId];

        var targetElem = $('.field-graph-container[data-chart-id="' + targetId + '"]');
        var draggedElem = $('.field-graph-container[data-chart-id="' + draggedId + '"]');

        var draggedOpts = JSON.parse(draggedElem.attr("data-lines"));

        // Update title and description.
        $(".title", targetElem).text("Combined chart");

        for (var i = 0; i < draggedChart.series.length; i++) {
            var lineColor = palette.color();
            var series = draggedChart.series[i];
            var query = series.gl2_query;

            if (query == undefined || query == "") {
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

    // Load all pinned charts
    (function() {
        if ($("#field-graphs").length > 0) {
            var charts = getPinnedCharts();

            // Are there even any pinned charts?
            if (Object.keys(charts).length > 0) {
                // Whoop whoop, pinned charts! Display the clear button first.
                $(".clear-pinned-charts").show();

                // Display all charts.
                for(var id in charts) {
                    var chart = charts[id];
                    chart.pinned = true;

                    renderFieldChart(chart);
                }
            }
        }
    })();

    function getPinnedCharts() {
        var pinned = store.get("pinned-field-charts");
        if (pinned == undefined) {
            pinned = {};
        }

        return pinned;
    }

    function setPinnedCharts(pinned) {
        store.set("pinned-field-charts", pinned);
    }

    function unpinChart(id) {
        var pinned = getPinnedCharts();
        delete pinned[id];
        setPinnedCharts(pinned);
    }

});