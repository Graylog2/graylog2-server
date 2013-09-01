$(document).ready(function() {

    $(".open-analyze-field").on("click", function() {
        $(".analyze-field", $(this).parent()).toggle();
        $(this).toggleClass("open-analyze-field-active");
    });

    $(".analyze-field .generate-overview").on("click", function() {
        var container = $(this).parent();
        $(this).attr("disabled", "disabled");

        showOverview($(this).attr("data-field"), container);
    })

    function showOverview(field, container) {
        var overview = $(".overview", container);

        $.ajax({
            url: '/a/search/fieldstats',
            data: {
                "timerange": originalSearchTimerange(),
                "q": originalSearchQuery(),
                "field": field
            },
            success: function(data) {
                overview.show();
                $(".analyzer-content", container).show();
                $("dd.count", overview).text(data.count);
                $("dd.mean", overview).text(data.mean.toFixed(2));
                $("dd.stddev", overview).text(data.std_deviation.toFixed(2));
                $("dd.min", overview).text(data.min);
                $("dd.max", overview).text(data.max);
                $("dd.sum", overview).text(data.sum.toFixed(2));
                $("dd.variance", overview).text(data.variance.toFixed(2));
                $("dd.squares", overview).text(data.sum_of_squares.toFixed(2));
            },
            statusCode: { 400: function() {
                $(".wrong-type", overview).show();
                overview.show();
            }},
            error: function(data) {
               if(data.status != 400) {
                    overview.hide();
                    showError("Could not load field statistics.");
               }
            },
            complete: function() {
                $(".spinner", container).hide();
            }
        });
    }

});