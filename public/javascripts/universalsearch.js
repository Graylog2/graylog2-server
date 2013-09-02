$(document).ready(function() {

    $(".universalsearch-timerange-selector input[name='timerange-selector-type']").on("change", function() {
        $(".timerange-selector").hide();
        $(".timerange-selector").attr("disabled", "disabled");

        var selected = $("." + $(this).val(), $(".universalsearch-form"));
        selected.show();
        selected.removeAttr("disabled");
    });

});