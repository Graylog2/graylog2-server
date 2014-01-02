$(document).ready(function() {

    $(".analyze-field .generate-graph .pie-chart").on("click", function(e) {
        e.preventDefault();

        // TODO
        alert("Pie charts are not implemented yet. (GitHub issue: #259)");
    });

    $(".analyze-field .generate-graph .line-chart").on("click", function(e) {
        e.preventDefault();

        opts = {}

        var container = $(this).closest(".analyze-field");
        if (!!container.attr("data-stream-id")) {
            opts["streamid"] = container.attr("data-stream-id");
        }

        renderFieldChart($(this).parent().parent().parent().attr("data-field"), opts);

        $(this).closest("li").addClass("disabled");
    });

    function renderFieldChart(field, opts) {
        if (opts == undefined) {
            opts = {};
        }

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

        var rangeType = $("#universalsearch-rangetype-permanent").text().trim();
        var query = $("#universalsearch-query-permanent").text().trim();

        var params = {
            "rangetype": rangeType,
            "q": query,
            "field": field,
            "interval": opts.interval,
            "valueType": opts.valuetype,
            "streamId": opts.streamid
        }

        switch(rangeType) {
            case "relative":
                params["relative"] = $("#universalsearch-relative-permanent").text();
                break;
            case "absolute":
                params["from"] = $("#universalsearch-from-permanent").text();
                params["to"] = $("#universalsearch-to-permanent").text();
                break;
            case "keyword":
                params["keyword"] = $("#universalsearch-keyword-permanent").text();
                break;
        }

        $("#field-graphs .spinner").show();

        /*
         * TODO:
         *   - export to image, ...
         *   - persist in localstorage?
         *   - overflowing select box
         *   - add multiple lines?
         */

        // Delete a possibly already existing graph of this value.
        $('.field-graph-container[data-field="' + field + '"]', $("#field-graphs")).remove();

        $.ajax({
            url: '/a/search/fieldhistogram',
            data: params,
            success: function(data) {
                var template = $("#field-graph-template").clone();
                template.removeAttr("id");
                template.attr("data-field", field);
                template.css("display", "block");
                $("h3 .title", template).text(field);
                $("ul", template).attr("data-field", field);

                template.attr("data-chart-id", opts.chartid);
                template.attr("data-config-interval", opts.interval);
                template.attr("data-config-interpolation", opts.interpolation);
                template.attr("data-config-renderer", opts.renderer);
                template.attr("data-config-valuetype", opts.valuetype);
                template.attr("data-config-streamid", opts.streamid);
                template.attr("data-config-pinned", opts.pinned);

                $(".type-description", template).text("(" + opts.valuetype + ")");

                if(opts.pinned) {
                    $(".pin", template).hide();
                    $(".unpin", template).show();
                }

                // Place the chart after all others but before the spinner.
                $("#field-graphs .spinner").before(template);

                var graphContainer = $('.field-graph-container[data-field="' + field + '"]', $("#field-graphs"));
                var graphElem = $('.field-graph', graphContainer);

                var graph = new Rickshaw.Graph( {
                    element: graphElem.get()[0],
                    width: $("#main-content").width()-12,
                    height: 175,
                    interpolation: opts.interpolation,
                    renderer: opts.renderer,
                    series: [ {
                        name: "value",
                        data: data.values,
                        color: '#26ADE4'
                    } ]
                });

                new Rickshaw.Graph.Axis.Y( {
                    graph: graph,
                    tickFormat: Rickshaw.Fixtures.Number.formatKMBT
                });

                new Rickshaw.Graph.Axis.Time({
                    graph: graph,
                    ticksTreatment: "glow"
                });

                new Rickshaw.Graph.HoverDetail({
                    graph: graph,
                    formatter: function(series, x, y) {
                        var date = '<span class="date">' + new Date(x * 1000).toUTCString() + '</span>';
                        var swatch = '<span class="detail_swatch"></span>';
                        var content = '[' + opts.valuetype + '] ' + field + ': ' + parseInt(y) + '<br>' + date;
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
                $(".field-graph-container ul.interval-selector li a").removeClass("selected");
                $('.field-graph-container ul.interval-selector li a[data-type="' + opts.interval + '"]').addClass("selected");

                $(".field-graph-container ul.interpolation-selector li a").removeClass("selected");
                $('.field-graph-container ul.interpolation-selector li a[data-type="' + opts.interpolation + '"]').addClass("selected");

                $(".field-graph-container ul.type-selector li a").removeClass("selected");
                $('.field-graph-container ul.type-selector li a[data-type="' + opts.renderer + '"]').addClass("selected");

                $(".field-graph-container ul.valuetype-selector li a").removeClass("selected");
                $('.field-graph-container ul.valuetype-selector li a[data-type="' + opts.valuetype + '"]').addClass("selected");

                fieldGraphs[field] = graph;

                // Is this chart pinned? We need to update it's settings then.
                var pinned = getPinnedCharts();
                if (pinned[opts.chartid] != undefined) {
                    pinned[opts.chartid] = opts;
                    setPinnedCharts(pinned);
                }
            },
            error: function(data) {
                if(data.status != 400) {
                    showError("Could not load histogram.");
                }
            },
            statusCode: { 400: function() {
                alert("Line charts are only available for numeric field types.");
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

        var graph = fieldGraphs[field];
        var type = $(this).attr("data-type");
        graph.setRenderer(type);

        if (type == "scatterplot") {
            graph.renderer.dotSize = 2;
        }

        if (type == "area") {
            graph.renderer.stroke = true;
        }

        graph.render();

        var graphContainer = $('.field-graph-container[data-field="' + field + '"]', $("#field-graphs"));
        graphContainer.attr("data-config-renderer", type);

        // Is this chart pinned? We need to update it's settings then.
        var pinned = getPinnedCharts();
        var chartId = graphContainer.attr("data-chart-id");
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

        var graph = fieldGraphs[field];
        graph.interpolation = interpolation;
        graph.render();

        var graphContainer = $('.field-graph-container[data-field="' + field + '"]', $("#field-graphs"));
        graphContainer.attr("data-config-interpolation", interpolation);

        // Is this chart pinned? We need to update it's settings then.
        var pinned = getPinnedCharts();
        var chartId = graphContainer.attr("data-chart-id");
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
        var graphContainer = $('.field-graph-container[data-field="' + field + '"]', $("#field-graphs"));

        var interval = $(this).attr("data-type");
        var opts = chartOptionsFromContainer(graphContainer);
        opts.interval = interval;

        renderFieldChart(field, opts);

        graphContainer.attr("data-config-interval", interval);

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Changing value type of value graphs.
    $(".field-graph-container ul.valuetype-selector li a").live("click", function(e) {
        e.preventDefault();
        var field = $(this).closest("ul").attr("data-field");
        var graphContainer = $('.field-graph-container[data-field="' + field + '"]', $("#field-graphs"));

        var valuetype = $(this).attr("data-type");
        var opts = chartOptionsFromContainer(graphContainer);
        opts.valuetype = valuetype;

        renderFieldChart(field, opts);

        graphContainer.attr("data-config-valuetype", valuetype);

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Removing a value graph.
    $(".field-graph-container li a.hide").live("click", function(e) {
        e.preventDefault();
        var field = $(this).closest("ul").attr("data-field");
        var graphContainer = $('.field-graph-container[data-field="' + field + '"]', $("#field-graphs"));

        unpinChart(graphContainer.attr("data-chart-id"));

        graphContainer.remove();
        delete fieldGraphs[field];


        // Enable chart generator link so we can generate this chart from again later.
        $("a.line-chart", $('div.generate-graph[data-field="' + field + '"]')).closest("li").removeClass("disabled");
    });

    function chartOptionsFromContainer(cc) {
        return {
            field: cc.attr("data-field"),
            chartid: cc.attr("data-chart-id"),
            interval: cc.attr("data-config-interval"),
            renderer: cc.attr("data-config-renderer"),
            interpolation: cc.attr("data-config-interpolation"),
            streamid: cc.attr("data-config-streamid"),
            valuetype: cc.attr("data-config-valuetype")
        }
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
        graphElem.attr("data-config-pinned", "false");

        unpinChart(graphElem.attr("data-chart-id"));

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

    // Load all pinned charts
    (function() {
        var charts = getPinnedCharts();

        // Are there even any pinned charts?
        if (Object.keys(charts).length > 0) {
            // Whoop whoop, pinned charts! Display the clear button first.
            $(".clear-pinned-charts").show();

            // Display all charts.
            for(var id in charts) {
                var chart = charts[id];
                chart.pinned = true;

                renderFieldChart(chart.field, chart);

                // Disable chart generator link so we can't generate this chart from scratch again (until unpinned).
                $("a.line-chart", $('div.generate-graph[data-field="' + chart.field + '"]')).closest("li").addClass("disabled");
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