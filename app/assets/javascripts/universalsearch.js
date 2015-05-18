$(document).ready(function() {

    $(".timerange-chooser .dropdown-menu a").on("click", function() {
        var selectorName = $(this).attr("data-selector-name");
        var link = $(this);

        activateTimerangeChooser(selectorName, link);
    });

    $("#universalsearch .timerange-selector-container .absolute .date-select").datepicker({
        format: "yyyy-mm-dd",
        weekStart: 1
    }).on("changeDate", function(ev) {
        var dateString = $(this).val() + " 00:00:00";
        var date = momentHelper.parseUserLocalFromString(dateString);
        $(this).val(date.format(momentHelper.DATE_FORMAT_TZ));
    });

    $("#universalsearch .timerange-selector-container .absolute .set-to-now").on("click", function() {
        var input = $("input", $(this).parent());

        var date = momentHelper.toUserTimeZone(null);
        input.val(date.format(momentHelper.DATE_FORMAT_TZ));
    });

    // on submit create the iso8601 timestamps for absolute searches
    $(".universalsearch-form").on("submit", function() {
        $(".timerange-selector-container .absolute input[type='text']").each(function() {
            var dateString = $(this).val();
            if (dateString) {
                var date = momentHelper.parseFromString(dateString);

                $("input[type='hidden']", $(this).parent()).val(date.toISOString());
            }
        });
    });

    // on submit fill the 'fields' field with the current viewstate
    $(".universalsearch-form").on("submit", function(){
        $("#universalsearch-fields", $(this)).val(searchViewState.getFieldsString());
    });

    $(".universalsearch-form").on("submit", function() {
        var width = $(document).width();
        $("input[name='width']", $(this)).val(width);
    });

    $(".added-width-search-link").on("click", function() {
        var width = $(document).width();
        var href = $(this).attr("href");
        href = URI(href).addSearch("width", width).toString();
        $(this).attr("href", href);
    });

    // initialize the user readable dates on page load
    $("#universalsearch .timerange-selector-container .absolute input[type='hidden']").each(function() {
        var input = $("input[type='text']", $(this).parent());
        var dateString = $(this).val();
        if (dateString) {
            var date = momentHelper.parseFromString(dateString);
            date = momentHelper.toUserTimeZone(date);
            input.val(date.format(momentHelper.DATE_FORMAT_TZ));
        }
    });

    $("#universalsearch .timerange-selector-container .keyword input").typeWatch({
        callback: function (string) {
            var preview = $("#universalsearch .timerange-selector-container .keyword .keyword-preview");
            if (string.length == 0) {
                preview.hide();
                return;
            }

            $.ajax({
                url: appPrefixed('/a/tools/natural_date_test'),
                data: {
                    "string": string
                },
                success: function(data) {
                    $(".not-recognized").hide();
                    var fromMoment = momentHelper.toUserTimeZone(data.from);
                    var toMoment = momentHelper.toUserTimeZone(data.to);

                    $(".from", preview).text(fromMoment.format("YYYY-MM-DD HH:mm:ss"));
                    $(".to", preview).text(toMoment.format("YYYY-MM-DD HH:mm:ss"));
                    $(".fromto", preview).show();
                },
                statusCode: { 422: function() {
                    $(".fromto", preview).hide();
                    $(".not-recognized").show();
                }},
                complete: function() {
                    preview.show();
                }
            });
        },
        wait: 500,
        highlight: true,
        captureLength: 0
    });


    $(".toggle-result-highlight").on("change", function(e) {
        $(".result-highlight").toggleClass("result-highlight-colored", $(this).is(":checked"));
    });

    // Save a search: Open save dialog.
    $(".save-search").on("click", function(e) {
        e.preventDefault();

        $(this).hide();

        var saveSearchForm = $(".save-search-form");
        saveSearchForm.show();
        focusFirstFormInput(saveSearchForm);
    });

    // Save a search: Ask for title and actually trigger saving.
    $(".save-search-form button.do-save").on("click", function(e) {
        e.preventDefault();

        var input = $("#save-search-title");
        var button = $(this);
        var title = input.val();

        button.prop("disabled", true);
        input.prop("disabled", true);

        button.html("<i class='fa fa-spin fa-spinner'></i>&nbsp; Saving");

        var params = {};
        params.query = originalUniversalSearchSettings(searchViewState);
        params.title = title

        $.ajax({
            url: appPrefixed('/savedsearches/create'),
            type: 'POST',
            data: {
                "params": JSON.stringify(params)
            },
            success: function(data) {
                button.html("<i class='fa fa-check'></i>&nbsp; Saved");
            },
            error: function(data) {
                button.html("<i class='fa fa-warning'></i>&nbsp; Failed");
                button.switchClass("btn-success", "btn-danger");
                showError("Could not save search.")
            }
        });

    });

    // Enable save button when text is entered.
    $("#save-search-title").on("keyup", function(e) {
        var button = $(".save-search-form button.do-save");
        if($(this).val() != undefined && $(this).val().trim() != "") {
            button.prop("disabled", false);
        } else {
            button.prop("disabled", true);
        }
    });

    // Saved search selected. Get details and send to page that redirects to the actual search.
    $("#saved-searches-selector").on("change", function(e) {
        var width = $(document).width();
        var searchId = $("#saved-searches-selector").val();

        var container = $(this).closest(".saved-searches-selector-container");
        if(!!container.attr("data-stream-id")) {
            var url = "/savedsearches/" + encodeURI(searchId) + "/execute?" + "streamId=" + container.attr("data-stream-id");
            if(width != null && width != '') {
                url += "&width=" + width;
            }
        } else {
            var url = "/savedsearches/" + encodeURI(searchId) + "/execute";
            if(width != null && width != '') {
                url += "?width=" + width;
            }
        }

        window.location = appPrefixed(url);
    });

    // Fill saved searches selector.
    if ($("#saved-searches-selector").size() > 0) {
        $.ajax({
            url: appPrefixed('/savedsearches'),
            type: 'GET',
            success: function(data) {
                // Convert to array for sorting.
                var array = $.map(data, function(value, index) {
                    return [value];
                });

                if (array.length > 0) {
                    // fml, js
                    array.sort(function(a, b) {
                        var textA = a.title.toUpperCase();
                        var textB = b.title.toUpperCase();
                        return (textA < textB) ? -1 : (textA > textB) ? 1 : 0;
                    });

                    for (var i = 0; i < array.length; i++) {
                        var search = array[i];
                        var option = "<option value='" + htmlEscape(search.id) + "'>" + htmlEscape(search.title) + "</option>";
                        $("#saved-searches-selector").append(option);
                    }

                    $("#saved-searches-selector").show();
                    $("#saved-searches-selector").chosen({
                        disable_search_threshold: 3,
                        no_results_text: "No such search found:"
                    });
                }
            }
        });
    }

});

function activateTimerangeChooser(selectorName, link) {
    var timerange = $(".timerange-selector");
    timerange.hide();
    $("#universalsearch .timerange-selector-container .keyword .preview").hide();
    $("#universalsearch-rangetype").val(selectorName);
    $("input,select", timerange).attr("disabled", "disabled");

    var selected = $("." + selectorName, $(".universalsearch-form"));
    selected.show();
    $("input,select", selected).removeAttr("disabled");

    $("a", link.parent().parent()).removeClass("selected");
    link.addClass("selected");
}

function activateTimerangeChooserV2(rangeType, rangeParams) {
    "use strict";
    $(document).trigger('change-timerange.graylog.search', {rangeType: rangeType, rangeParams: rangeParams});
}

function submitSearch() {
    "use strict";
    $(document).trigger('execute.graylog.search');
}
