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

    $("#universalsearch .timerange-selector-container .keyword input").typeWatch({
        callback: function (string) {
            var preview = $("#universalsearch .timerange-selector-container .keyword .keyword-preview");
            if (string.length == 0) {
                preview.hide();
                return;
            }

            $.ajax({
                url: '/a/tools/natural_date_test',
                data: {
                    "string": string,
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