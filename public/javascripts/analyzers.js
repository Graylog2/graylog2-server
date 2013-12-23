$(document).ready(function() {

    $(".open-analyze-field").on("click", function(e) {
        e.preventDefault();

        $(".analyze-field", $(this).parent()).toggle();
        $(this).toggleClass("open-analyze-field-active");
    });

    $(".analyze-field .generate-statistics").on("click", function(e) {
        e.preventDefault();

        var container = $(this).parent();
        $(this).attr("disabled", "disabled");

        showStatistics($(this).attr("data-field"), container);
    })

    $(".analyze-field .show-quickvalues").on("click", function(e) {
        if ($(this).attr("disabled") == "disabled") {
            return;
        }

        e.preventDefault();

        // Hide and disable all others.
        $(".quickvalues").hide();
        $(".show-quickvalues").removeAttr("disabled");
        $(".quickvalues").attr("data-active", "false");

        // Mark this one as active.
        $(".quickvalues", $(this).parent()).attr("data-active", "true");

        $(this).attr("disabled", "disabled");

        showQuickValues($(this).attr("data-field"), $(this).parent(), true, calculateDirection($(this)), true);
    });

    $(".quickvalues .quickvalues-refresh").on("click", function(e) {
        e.preventDefault();

        var button = $(".analyze-field .show-quickvalues[data-field=" + $(this).attr("data-field") + "]");
        var quickvalues = $(".quickvalues[data-field=" + $(this).attr("data-field") + "]");

        showQuickValues($(this).attr("data-field"), quickvalues.parent(), true, calculateDirection(button), false);
    });

    $(".quickvalues .quickvalues-export").on("click", function(e) {
        e.preventDefault();

        // TODO
        alert("Exporting statistics is not implemented yet. (GitHub issue: #239)");
    });

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
    });

    $(".quickvalues .quickvalues-close").on("click", function(e) {
        e.preventDefault();

        var quickvalues = $(".quickvalues[data-field=" + $(this).attr("data-field") + "]");
        var button = $(".analyze-field .show-quickvalues[data-field=" + $(this).attr("data-field") + "]");

        button.removeAttr("disabled");

        quickvalues.attr("data-active", "false");
        quickvalues.hide();
    });

    $(".quickvalues .quickvalues-autorefresh").on("click", function(e) {
        e.preventDefault();
        var quickvalues = $(".quickvalues[data-field=" + $(this).attr("data-field") + "]");;

        if ($(this).hasClass("active")) {
            // Disabling autorefresh.
            quickvalues.attr("data-autorefresh", "false");
            $(this).removeClass("active");
        } else {
            // Enabling autorefresh.
            quickvalues.attr("data-autorefresh", "true");
            $(this).addClass("active");

            // Load once to trigger reload cycle again.
            var button = $(".analyze-field .show-quickvalues[data-field=" + $(this).attr("data-field") + "]");
            showQuickValues($(this).attr("data-field"), quickvalues.parent(), true, calculateDirection(button), true);
        }
    });

    function showStatistics(field, container) {
        var statistics = $(".statistics", container);

        // TODO: deduplicate
        var rangeType = $("#universalsearch-rangetype-permanent").text().trim();
        var query = $("#universalsearch-query-permanent").text().trim();

        var params = {
            "rangetype": rangeType,
            "q": query,
            "field": field
        }

        if(!!container.attr("data-stream-id")) {
            params["stream_id"] = container.attr("data-stream-id");
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

        $.ajax({
            url: '/a/search/fieldstats',
            data: params,
            success: function(data) {
                statistics.show();
                $(".analyzer-content", container).show();
                $("dd.count", statistics).text(data.count);
                $("dd.mean", statistics).text(data.mean.toFixed(2));
                $("dd.stddev", statistics).text(data.std_deviation.toFixed(2));
                $("dd.min", statistics).text(data.min);
                $("dd.max", statistics).text(data.max);
                $("dd.sum", statistics).text(data.sum.toFixed(2));
                $("dd.variance", statistics).text(data.variance.toFixed(2));
                $("dd.squares", statistics).text(data.sum_of_squares.toFixed(2));
            },
            statusCode: { 400: function() {
                $(".wrong-type", statistics).show();
                statistics.show();
            }},
            error: function(data) {
               if(data.status != 400) {
                    statistics.hide();
                    showError("Could not load field statistics.");
               }
            },
            complete: function() {
                $(".spinner", container).hide();
            }
        });
    }

    function showQuickValues(field, container, manualReload, direction, reload) {
        var quickvalues = $(".quickvalues", container);

        // Never update anything if this is a non-visible reload and we are not active or auto-refresh is disabled.
        // Prevents unneeded calculations when the windows is hidden and auto-refresh is enabled.
        if (reload && (quickvalues.attr("data-active") != "true" || quickvalues.attr("data-autorefresh") == "false")) {
            return;
        }

        var inlineSpin = "<i class='icon icon-spinner icon-spin'></i>";

        if (manualReload) {
            $(".terms-total", quickvalues).html(inlineSpin);
            $(".terms-missing", quickvalues).html(inlineSpin);
            $(".terms-other", quickvalues).html(inlineSpin);

            $(".terms tbody", quickvalues).empty();
            $(".terms tbody", quickvalues).append("<tr><td colspan='3'>" + inlineSpin + "</td></tr>");

            $(".terms-distribution", quickvalues).hide();
        }

        var button = $(".analyze-field .show-quickvalues[data-field=" + field + "]");
        updatePosition(button, quickvalues, direction);

        switch(direction)  {
            case "up":
                quickvalues.removeClass("quickvalues-down");
                quickvalues.addClass("quickvalues-up");
                break;
            case "down":
                quickvalues.removeClass("quickvalues-up");
                quickvalues.addClass("quickvalues-down");
                break;
        }

        quickvalues.show();

        if(reload) {
            $(".quickvalues-autorefresh", quickvalues).addClass("loading");
        }

        var rangeType = $("#universalsearch-rangetype-permanent").text().trim();
        var query = $("#universalsearch-query-permanent").text().trim();

        var params = {
            "rangetype": rangeType,
            "q": query,
            "field": field
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

        if(!!container.attr("data-stream-id")) {
            params["stream_id"] = container.attr("data-stream-id");
        }

        $.ajax({
            url: '/a/search/fieldterms',
            data: params,
            success: function(data) {
                $(".terms-total", quickvalues).text(data.total);
                $(".terms-other", quickvalues).text(data.other);
                $(".terms-missing", quickvalues).text(data.missing);

                // Remove all items before writing again.
                $(".terms tbody", quickvalues).empty();
                $(".terms-distribution", quickvalues).empty();

                $(".terms-distribution", quickvalues).show();

                var colors = d3.scale.category20c();

                sortedKeys = Object.keys(data.terms).sort(function(a,b){return data.terms[b] - data.terms[a]});

                for(var i = 0; i < sortedKeys.length; i++){
                    var key = sortedKeys[i];
                    var val = data.terms[key];

                    var percent = (val/data.total*100);

                    var searchLink = "<a href='#' title='Search for this value. (Press alt to search immediately, shift to negate)' class='search-link' data-field='" + htmlEscape(field) + "' data-value='" + htmlEscape(key) + "'><i class='icon icon-search'></i></a>";

                    $(".terms tbody", quickvalues).append("<tr data-i='" + i + "' data-name='" + htmlEscape(key) + "'><td>"+ searchLink +"</td><td>" + htmlEscape(key) + "</td><td>" + percent.toFixed(2) + "%</td><td>" + htmlEscape(val) + "</td></tr>");
                    $(".terms-distribution", quickvalues).append("<div class='terms-bar terms-bar-" + i + "' style='width: " + percent + "%; background-color: " + colors(i) + ";'></div>");
                }
            },
            error: function(data) {
                quickvalues.hide();
                showError("Could not load quick values.");
            },
            complete: function() {
                $(".nano", quickvalues).nanoScroller();

                if (reload) {
                    // Loading complete. Set autoreload button to old color again.
                    $(".quickvalues-autorefresh", quickvalues).removeClass("loading");

                    // Call everything again in 2.5sec
                    setTimeout(function() {
                        showQuickValues(field, container, false, direction, true);
                    }, 3000)
                }
            }
        });
    }

    // Quickterms table row highlighting.
    $(".quickvalues .terms tbody tr")
        .live("mouseenter", highlightTermsBar)
        .live("mouseleave", resetTermsBar);

    function highlightTermsBar() {
        var bar = $(".terms-bar-" + $(this).attr("data-i"));
        bar.attr("data-original-color", bar.css("background-color"));
        bar.css("background-color", "#dd514c");
    }

    function resetTermsBar() {
        var bar = $(".terms-bar-" + $(this).attr("data-i"));
        bar.css("background-color", bar.attr("data-original-color"));
    }

    function calculateDirection(linkel) {
        if (($(window).height() - linkel.offset().top) < 400) {
            return "up";
        } else {
            return "down";
        }
    }

    // Update all the positions, all the time.
    // Updating total event counts;
    (function updateAllPositions() {
        $(".quickvalues").each(function(i) {
            var button = $(".analyze-field .show-quickvalues[data-field=" + $(this).attr("data-field") + "]");
            updatePosition(button, $(this), calculateDirection(button));
        });

        setTimeout(updateAllPositions, 25);
    })();

    function updatePosition(button, quickvalues, direction) {

        var left = button.offset().left-$(window).scrollLeft()-622;

        switch(direction)  {
            case "up":
                var top = button.offset().top-$(window).scrollTop()-390;
                break;
            case "down":
                var top = button.offset().top-$(window).scrollTop()-20;
                break;
        }

        quickvalues.css("top", top);
        quickvalues.css("left", left);
    }

    function renderFieldChart(field, opts) {
        if (opts == undefined) {
            opts = {};
        }

        // Options.
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

                template.attr("data-config-interval", opts.interval);
                template.attr("data-config-interpolation", opts.interpolation);
                template.attr("data-config-renderer", opts.renderer);
                template.attr("data-config-valuetype", opts.valuetype);
                template.attr("data-config-streamid", opts.streamid);

                $(".type-description", template).text("(" + opts.valuetype + ")");

                $("#field-graphs").append(template);

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
                        var content = field + ': ' + parseInt(y) + '<br>' + date;
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

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Changing interval of value graphs.
    $(".field-graph-container ul.interval-selector li a").live("click", function(e) {
        e.preventDefault();
        var field = $(this).closest("ul").attr("data-field");
        var graphContainer = $('.field-graph-container[data-field="' + field + '"]', $("#field-graphs"));

        var interval = $(this).attr("data-type");

        renderFieldChart(field, {
            interval: interval,
            renderer: graphContainer.attr("data-config-renderer"),
            interpolation: graphContainer.attr("data-config-interpolation"),
            streamid: graphContainer.attr("data-config-streamid"),
            valuetype:  graphContainer.attr("data-config-valuetype")
        });

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

        renderFieldChart(field, {
            interval: graphContainer.attr("data-config-interval"),
            renderer: graphContainer.attr("data-config-renderer"),
            interpolation: graphContainer.attr("data-config-interpolation"),
            streamid: graphContainer.attr("data-config-streamid"),
            valuetype: valuetype
        });

        graphContainer.attr("data-config-valuetype", valuetype);

        $("a", $(this).closest("ul")).removeClass("selected");
        $(this).addClass("selected");
    });

    // Removing a value graph.
    $(".field-graph-container li a.hide").live("click", function(e) {
        e.preventDefault();
        var field = $(this).closest("ul").attr("data-field");
        var graphContainer = $('.field-graph-container[data-field="' + field + '"]', $("#field-graphs"));

        graphContainer.remove();
        delete fieldGraphs[field];
    });

    $(".field-graph-container .add-to-dashboard").live("click", function(e) {
        e.preventDefault();

        // TODO
        alert("Adding charts to dashboards is not implemented yet. (GitHub issue: #327)");
    });

    $(".field-graph-container .pin").live("click", function(e) {
        e.preventDefault();

        // TODO
        alert("Pinning/persisting charts is not implemented yet. (GitHub issue: #329)");
    });

});