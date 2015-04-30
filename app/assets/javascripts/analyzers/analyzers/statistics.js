$(document).ready(function() {

    $(".analyze-field .generate-statistics").on("click", function(e) {
        e.preventDefault();

        var container = $(this).parent();

        showStatistics($(this).attr("data-field"), container);
    });

    function normalizeNumber(number) {
        if (number === null || isNaN(number)) {
            return number;
        }
        return numeral(number).format("0,0.[00]");
    }

    function showStatistics(field, container) {
        var statistics = $(".statistics", container);
        statistics.hide();

        // TODO: deduplicate
        var rangeType = $("#universalsearch-rangetype-permanent").text().trim();
        var query = $("#universalsearch-query-permanent").text().trim();

        var params = {
            "rangetype": rangeType,
            "q": query,
            "field": field
        };

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
            url: appPrefixed('/a/search/fieldstats'),
            data: params,
            success: function(data) {
                statistics.show();
                $(".analyzer-content", container).show();
                $("dd.count", statistics).text(normalizeNumber(data.count));
                $("dd.mean", statistics).text(normalizeNumber(data.mean.toFixed(2)));
                $("dd.stddev", statistics).text(normalizeNumber(data.std_deviation.toFixed(2)));
                $("dd.min", statistics).text(normalizeNumber(data.min));
                $("dd.max", statistics).text(normalizeNumber(data.max));
                $("dd.sum", statistics).text(normalizeNumber(data.sum.toFixed(2)));
                $("dd.variance", statistics).text(normalizeNumber(data.variance.toFixed(2)));
                $("dd.squares", statistics).text(normalizeNumber(data.sum_of_squares.toFixed(2)));
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

});