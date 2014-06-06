$(document).ready(function() {

    $(".timerange-chooser .dropdown-menu a").on("click", function() {
        var selectorName = $(this).attr("data-selector-name");
        var link = $(this);

        activateTimerangeChooser(selectorName, link);
    });

    $("#universalsearch .timerange-selector-container .absolute .date-select").datepicker({
        format: "yyyy-mm-dd"
    }).on("changeDate", function(ev) {
        $(this).val($(this).val() + " 00:00:00");
    });

    $("#universalsearch .timerange-selector-container .absolute .set-to-now").on("click", function() {
        var input = $("input", $(this).parent());

        var date = new Date();
        input.val(searchDateTimeFormatted(date));
    });

    // on submit create the iso8601 timestamps for absolute searches
    $(".universalsearch-form").on("submit", function() {
        $(".timerange-selector-container .absolute input[type='text']").each(function() {
            var dateString = $(this).val();
            if (dateString) {
                var date = new Date(parseDateFromString(dateString));

                $("input[type='hidden']", $(this).parent()).val(date.toISOString());
            }
        });
    });

    // on submit fill the 'fields' field with the current viewstate
    $(".universalsearch-form").on("submit", function(){
        $("#universalsearch-fields", $(this)).val(searchViewState.getFieldsString());
    });

    // initialize the user readable dates on page load
    $("#universalsearch .timerange-selector-container .absolute input[type='hidden']").each(function() {
        var input = $("input[type='text']", $(this).parent());
        var dateString = $(this).val();
        if (dateString) {
            var date = new Date(dateString);
            input.val(searchDateTimeFormatted(date));
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

                    $(".from", preview).text(data.from);
                    $(".to", preview).text(data.to);
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

    /* TODO figure out if we want to keep it this way */
    String.prototype.gl2_splice = function( idx, s ) {
        return (this.slice(0,idx) + s + this.slice(idx));
    };
    $(".messages tbody tr").each(function() {
        var ranges = $(this).data('highlight');

        if (ranges == undefined) {
            // Search highlighting not enabled in server.
            $(".explain-result-highlight-control").show();
        } else {
            // Search highlighting is enabled in server.
            for (var field in ranges) {
                if (! ranges.hasOwnProperty(field) ) {
                    continue;
                }
                var positions = ranges[field];
                var fieldNameHash = CryptoJS.MD5(field);
                $(".result-td-" + fieldNameHash, $(this)).each(function(){
                    var elemText = $(this).text();
                    for (var idx = positions.length - 1; idx >= 0; idx--) {
                        var range = positions[idx];
                        elemText = elemText.gl2_splice(range.start + range.length, "</span>");
                        elemText = elemText.gl2_splice(range.start, '<span class="result-highlight">');
                    }
                    $(this).html(elemText);
                    $(".result-highlight", $(this)).toggleClass("result-highlight-colored");
                });
                $(".result-highlight-control").show();
            }
        }
    });

    // Save a search: Open save dialog.
    $(".save-search").on("click", function(e) {
        e.preventDefault();

        $(this).hide();
        $(".save-search-form").show();
    });

    // Save a search: Ask for title and actually trigger saving.
    $(".save-search-form button.do-save").on("click", function(e) {
        e.preventDefault();

        var input = $("#save-search-title");
        var button = $(this);
        var title = input.val();

        button.prop("disabled", true);
        input.prop("disabled", true);

        button.html("<i class='icon icon-spin icon-spinner'></i>&nbsp; Saving");

        var params = {};
        params.query = originalUniversalSearchSettings();
        params.title = title

        $.ajax({
            url: appPrefixed('/savedsearches/create'),
            type: 'POST',
            data: {
                "params": JSON.stringify(params)
            },
            success: function(data) {
                button.html("<i class='icon icon-ok'></i>&nbsp; Saved");
            },
            error: function(data) {
                button.html("<i class='icon icon-warning-sign'></i>&nbsp; Failed");
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
        var searchId = $("#saved-searches-selector").val();

        var container = $(this).closest(".saved-searches-selector-container");
        if(!!container.attr("data-stream-id")) {
            var url = "/savedsearches/" + encodeURI(searchId) + "/execute?" + "streamId=" + container.attr("data-stream-id");
        } else {
            var url = "/savedsearches/" + encodeURI(searchId) + "/execute";
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
    $(".timerange-selector").hide();
    $("#universalsearch .timerange-selector-container .keyword .preview").hide();
    $("#universalsearch-rangetype").val(selectorName);
    $(".timerange-selector input,select").attr("disabled", "disabled");

    var selected = $("." + selectorName, $(".universalsearch-form"));
    selected.show();
    $("input,select", selected).removeAttr("disabled");

    $("a", link.parent().parent()).removeClass("selected");
    link.addClass("selected");
}