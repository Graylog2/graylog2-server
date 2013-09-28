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
        e.preventDefault();

        // Hide all others.
        $(".quickvalues").hide();

        var direction = "down";
        if (($(window).height() - $(this).offset().top) < 400) {
            direction = "up";
        }

        showQuickValues($(this).attr("data-field"), $(this).parent(), true, direction);
    });

    $(".quickvalues .quickvalues-refresh").on("click", function(e) {
        e.preventDefault();

        showQuickValues($(this).parent().parent().parent().attr("data-field"), $(this).parent().parent().parent().parent().parent(), true);
    });

    $(".quickvalues .quickvalues-export").on("click", function(e) {
        e.preventDefault();

        // TODO
        alert("Exporting statistics is not implemented yet. (Issue: #239)");
    });

    $(".quickvalues .quickvalues-close").on("click", function(e) {
        e.preventDefault();

        $(this).parent().parent().parent().hide();
    });

    function showStatistics(field, container) {
        var statistics = $(".statistics", container);

        // TODO: deduplicate
        var rangeType = $("#universalsearch-rangetype-permanent").text();
        var query = $("#universalsearch-query-permanent").text();

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

    function showQuickValues(field, container, visibleReload, direction) {
        var quickvalues = $(".quickvalues", container);

        if (!visibleReload && quickvalues.css("display") == "none") {
            // Call again in 2.5sec
            setTimeout(function() {
                showQuickValues(field, container, false, direction);
            }, 2500)

            return;
        }

        var inlineSpin = "<i class='icon icon-spinner icon-spin'></i>";

        if (visibleReload) {
            $(".terms-total", quickvalues).html(inlineSpin);
            $(".terms-missing", quickvalues).html(inlineSpin);
            $(".terms-other", quickvalues).html(inlineSpin);

            $(".terms tbody", quickvalues).empty();
            $(".terms tbody", quickvalues).append("<tr><td colspan='3'>" + inlineSpin + "</td></tr>");

            $(".terms-distribution", quickvalues).hide();
        }

        if (direction == "up") {
            quickvalues.removeClass("quickvalues-down");
            quickvalues.addClass("quickvalues-up");
        } else {
            quickvalues.removeClass("quickvalues-up");
            quickvalues.addClass("quickvalues-down");
        }

        quickvalues.show();

        /*
         * TODO:
         *
         *   - auto-reload enable/disable
         *   - show button as selected, second click closes again
         *
         */

        // TODO: deduplicate
        var rangeType = $("#universalsearch-rangetype-permanent").text();
        var query = $("#universalsearch-query-permanent").text();

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

                    $(".terms tbody", quickvalues).append("<tr data-i='" + i + "' data-name='" + key + "'><td>" + key + "</td><td>" + percent.toFixed(2) + "%</td><td>" + val + "</td></tr>");
                    $(".terms-distribution", quickvalues).append("<div class='terms-bar terms-bar-" + i + "' style='width: " + percent + "%; background-color: " + colors(i) + ";'></div>");
                }
            },
            error: function(data) {
                if(data.status != 400) {
                    statistics.hide();
                    showError("Could not load quick values.");
                }
            },
            complete: function() {
                $(".nano").nanoScroller();

                // Call again in 2.5sec
                setTimeout(function() {
                    showQuickValues(field, container, false, direction);
                }, 2500)
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

});