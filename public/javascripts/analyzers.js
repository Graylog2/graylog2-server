$(document).ready(function() {

    $(".open-analyze-field").on("click", function() {
        $(".analyze-field", $(this).parent()).toggle();
        $(this).toggleClass("open-analyze-field-active");
    });

    $(".analyze-field .generate-statistics").on("click", function() {
        var container = $(this).parent();
        $(this).attr("disabled", "disabled");

        showStatistics($(this).attr("data-field"), container);
    })

    $(".analyze-field .show-quickvalues").on("click", function() {
        showQuickValues($(this).attr("data-field"), $(this).parent(), true);
    });

    $(".quickvalues .quickvalues-refresh").on("click", function() {
        showQuickValues($(this).parent().parent().parent().attr("data-field"), $(this).parent().parent().parent().parent().parent(), true);
    });

    $(".quickvalues .quickvalues-close").on("click", function() {
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

    function showQuickValues(field, container, spin) {
        var quickvalues = $(".quickvalues", container);

        var inlineSpin = "<i class='icon icon-spinner icon-spin'></i>";

        if (spin) {
            $(".terms-total", quickvalues).html(inlineSpin);
            $(".terms-missing", quickvalues).html(inlineSpin);

            $(".terms tbody", quickvalues).empty();
            $(".terms tbody", quickvalues).append("<tr><td colspan='3'>" + inlineSpin + "</td></tr>");
        }

        quickvalues.show();

        /*
         * TODO:
         *
         *   - show and explain "other"
         *   - auto-reload
         *   - sort table values
         *   - different colors
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
                $(".terms-missing", quickvalues).text(data.missing);

                // Remove all items before writing again.
                $(".terms tbody", quickvalues).empty();
                $(".terms-distribution", quickvalues).empty();

                $(".terms-distribution", quickvalues).show();

                for(var term in data.terms){
                    var percent = (data.terms[term]/data.total*100).toFixed(2);

                    $(".terms tbody", quickvalues).append("<tr><td>" + term + "</td><td>" + percent + "%</td><td>" + data.terms[term] + "</td></tr>");
                    $(".terms-distribution", quickvalues).append("<div class='bar' style='width: " + percent + "%;'></div>");
                }
            },
            error: function(data) {
                if(data.status != 400) {
                    statistics.hide();
                    showError("Could not load quick values.");
                }
            },
            complete: function() {
                // TODO $(".spinner", container).hide();
            }
        });


    }

});