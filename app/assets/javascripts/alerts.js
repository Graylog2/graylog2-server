$(document).ready(function() {

    $(".add-alert").on("click", function() {
        $(".alert-type-form").hide();
        $("#" + $(".add-alert-type").val()).show();
    });

    $(".alert-type-form").on("submit", function(e) {
        return validate("#" + $(this).attr("id"));
    });

});

function fillAlertAnnotator(chart, annotator) {
    var oldestDataPoint = new Date().getTime(); // now.
    for (var i = 0; i < chart.series.length; i++) {
        var data = chart.series[i].data;
        if (data.length > 0) {
            if (oldestDataPoint > data[0].x) {
                oldestDataPoint = data[0].x;
            }
        }
    }

    // Get alerts since oldest data point and add to annotations.
    $.ajax({
        url: appPrefixed("/a/streams/alerts/allowed"),
        data: {
            since: oldestDataPoint
        },
        type: "GET",
        success: function(data) {
            for (var i = 0; i < data.alerts.length; i++) {
                var alert = data.alerts[i];

                annotator.add(alert.triggered_at, buildAlertAnnotationText(alert));
                annotator.update();
            }
        }
    });
}

function buildAlertAnnotationText(alert) {
    // lol solli
    return "<i class='icon icon-warning-sign'></i> Stream \"" + alert.stream_name + "\" triggered an alert: "
        + alert.description;

}