$(document).ready(function() {

    $(".add-alert").on("click", function(e) {
        $(".alert-type-form").hide();
        $("#" + $(".add-alert-type").val()).show();
        e.preventDefault();
    });

    $(".add-alert-destination").on("click", function() {
        $(".alert-destination-form").hide();
        $(".alert-destination-form[data-callback-type='" + $(".add-alert-destination-type").val() + "']").show();
    });

    $(".alert-type-form").on("submit", function(e) {
        return validate("#" + $(this).attr("id"));
    });

    $(".alert-destination-form").on("submit", function(e) {
        return validate("#" + $(this).attr("id"));
    });

    $(".cancel-destination-form").on("click", function(e) {
        $(".alert-destination-form").hide();
        e.preventDefault();
    });

    $(".edit-alarm-destination").on("click", function(e) {
        var callbackId = $(this).attr("data-callback-id");
    });

    $(".edit-alert-condition").on("click", function(e){
        var conditionId = $(this).closest(".alert-condition").data("condition-id");
        $("#alert-condition-" + conditionId).toggle();
        e.preventDefault();
    });

    var usernamesTypeaheadField = $("#add-alert-receivers #user");
    var usernameList = usernamesTypeaheadField.data("source");

    usernamesTypeaheadField.typeahead(
        {
            hint: true,
            highlight: true,
            minLength: 1
        },
        {
            name: 'usernames',
            displayKey: 'value',
            source: substringMatcher(usernameList, 'value')
        }
    );
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
    return "<i class='fa fa-warning'></i> Stream \"" + alert.stream_name + "\" triggered an alert: "
        + alert.description;

}